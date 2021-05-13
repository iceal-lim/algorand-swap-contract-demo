package cloud.blockchainsolutions.algorand.service;

import cloud.blockchainsolutions.algorand.domain.ContractResponse;
import cloud.blockchainsolutions.algorand.domain.Token;
import cloud.blockchainsolutions.algorand.domain.TokenPurchaseRequest;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.crypto.LogicsigSignature;
import com.algorand.algosdk.transaction.SignedTransaction;
import com.algorand.algosdk.transaction.Transaction;
import com.algorand.algosdk.transaction.TxGroup;
import com.algorand.algosdk.util.Encoder;
import com.algorand.algosdk.v2.client.common.AlgodClient;
import com.algorand.algosdk.v2.client.common.Response;
import com.algorand.algosdk.v2.client.model.CompileResponse;
import com.algorand.algosdk.v2.client.model.NodeStatusResponse;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.algorand.algosdk.v2.client.model.PostTransactionsResponse;
import com.algorand.algosdk.v2.client.model.TransactionParametersResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Charsets;
import com.google.common.collect.Maps;
import com.google.common.io.Resources;
import com.hubspot.jinjava.Jinjava;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;

@Service
@Slf4j
public class AlgorandClientService {

    @Value("${algorand.transaction.timeout:10}")
    private final int transactionDefaultTimeout = 10;

    private final AlgodClient algodClient;
    private final Jinjava jinJava;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AlgorandClientService(final AlgodClient algodClient, final Jinjava jinJava) {
        this.algodClient = algodClient;
        this.jinJava = jinJava;
    }

    public JsonNode printTransactionResponse(PendingTransactionResponse pendingTransactionResponse) throws JsonProcessingException {
        JsonNode jsonNode = objectMapper.readTree(pendingTransactionResponse.toString());
        log.info("PendingTransactionResponse : {}", jsonNode.toPrettyString());
        return jsonNode;
    }

    public PendingTransactionResponse optInASA(Token token, Account buyer) throws Exception {
        Transaction txn = Transaction.AssetAcceptTransactionBuilder()
                .acceptingAccount(buyer.getAddress())
                .assetIndex(token.getAssetId())
                .suggestedParams(getTxParams())
                .build();

        SignedTransaction signedTxn = buyer.signTransaction(txn);
        byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);
        return submitTransaction(encodedTxBytes);
    }

    public PendingTransactionResponse sendAlgoToAddress(Account buyer, String contractAddress, Long microAlgos, String note) throws Exception {
        Transaction txn = Transaction.PaymentTransactionBuilder()
                .sender(buyer.getAddress())
                .amount(microAlgos)
                .note(note.getBytes(StandardCharsets.UTF_8))
                .receiver(contractAddress)
                .suggestedParams(getTxParams())
                .build();

        SignedTransaction signedTxn = buyer.signTransaction(txn);
        byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);
        return submitTransaction(encodedTxBytes);
    }

    public ContractResponse createTokenPurchaseContract(Token token, String buyer, String seller) throws Exception {
        Response<NodeStatusResponse> nodeStatus = algodClient.GetStatus().execute();
        if(!nodeStatus.isSuccessful()) {
            throw new RuntimeException("unable to determine the node status " + nodeStatus.message());
        }
        Long lastRound = nodeStatus.body().lastRound;
        Map<String, Object> context = Maps.newHashMap();

        context.put("round", lastRound);
        context.put("assetId", token.getAssetId());
        context.put("buyer", buyer);
        context.put("owner", seller);
        context.put("fee", 2000);

        String template = Resources.toString(Resources.getResource("teal/escrow_asset.teal"), Charsets.UTF_8);

        String programSource = jinJava.render(template, context);
        log.info("programSource:\n{}", programSource);

        CompileResponse response = algodClient.TealCompile().source(programSource.getBytes()).execute().body();
        log.info("response: " + response);
        log.info("Hash: " + response.hash);
        log.info("Result: " + response.result);

        byte[] program = Base64.getDecoder().decode(response.result);

        return ContractResponse.builder()
                .address(response.hash)
                .logicsigSignature(new LogicsigSignature(program, null))
                .round(lastRound).build();
    }

    public PendingTransactionResponse contractOptInASA(ContractResponse contractResponse, Token token, String buyerAddress, String sellerAddress) throws Exception {

        Map<String, Object> context = Maps.newHashMap();
        context.put("round", contractResponse.getRound());
        context.put("assetId", token.getAssetId());
        context.put("buyer", buyerAddress);
        context.put("owner", sellerAddress);
        context.put("fee", 2000);

        Transaction txn = Transaction.AssetAcceptTransactionBuilder()
                .acceptingAccount(contractResponse.getAddress())
                .assetIndex(token.getAssetId())
                .suggestedParams(getTxParams())
                .build();

        SignedTransaction signedTxn = Account.signLogicsigTransaction(contractResponse.getLogicsigSignature(), txn);

        byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);

        return submitTransaction(encodedTxBytes);
    }

    public PendingTransactionResponse sendTokenToAddress(Account seller, String contractAddress, long numberOfTokens, long assetId) throws Exception {
        Transaction txn = Transaction.AssetTransferTransactionBuilder()
                .sender(seller.getAddress())
                .assetReceiver(contractAddress)
                .assetAmount(numberOfTokens)
                .assetIndex(assetId)
                .suggestedParams(getTxParams())
                .build();

        SignedTransaction signedTxn = seller.signTransaction(txn);
        byte[] encodedTxBytes = Encoder.encodeToMsgPack(signedTxn);

        return submitTransaction(encodedTxBytes);
    }

    public PendingTransactionResponse executeSwapContract(ContractResponse contractResponse, Token token, TokenPurchaseRequest purchaseRequest,
                                    String buyerAddress, String sellerAddress) throws Exception {
        TransactionParametersResponse params = getTxParams();

        Transaction txn1 = Transaction.AssetTransferTransactionBuilder()
                .sender(contractResponse.getAddress())
                .assetReceiver(buyerAddress)
                .assetAmount(purchaseRequest.getNumberOfTokens())
                .assetIndex(token.getAssetId())
                .suggestedParams(params)
                .build();

        log.info("sending amount: {} from: {} to: {} remainder: {}", purchaseRequest.getNumberOfTokens(),
                contractResponse.getAddress(), buyerAddress, sellerAddress);

        long totalAmount = purchaseRequest.getNumberOfTokens() * token.getPriceInMicroAlgo();

        Transaction txn2 = Transaction.PaymentTransactionBuilder()
                .sender(contractResponse.getAddress())
                .amount(totalAmount)
                .receiver(sellerAddress)
                .suggestedParams(params)
                .build();

        log.info("sending amount: {} from: {} to: {} remainder: {}", totalAmount, contractResponse.getAddress(),
                sellerAddress, buyerAddress);

        // sign transaction
        TxGroup.assignGroupID(txn1, txn2);

        LogicsigSignature lsig = contractResponse.getLogicsigSignature();
        SignedTransaction signedTxn1 = Account.signLogicsigTransaction(lsig, txn1);
        SignedTransaction signedTxn2 = Account.signLogicsigTransaction(lsig, txn2);

        log.info("stateless contract signed transaction with txid1: {}, txid2: {}", signedTxn1.transactionID, signedTxn2.transactionID);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        baos.write(Encoder.encodeToMsgPack(signedTxn1));
        baos.write(Encoder.encodeToMsgPack(signedTxn2));
        byte[] encodedTxBytes = baos.toByteArray();

        return submitTransaction(encodedTxBytes);
    }


    private TransactionParametersResponse getTxParams() throws Exception {
        TransactionParametersResponse params = algodClient.TransactionParams().execute().body();
        params.minFee = (long)1000;
        params.fee = (long)1;
        return params;
    }

    private PendingTransactionResponse submitTransaction(byte[] encodedTxBytes) throws Exception {
        Response<PostTransactionsResponse> postTransactionsResponseResponse = algodClient.RawTransaction().rawtxn(encodedTxBytes).execute();

        if(!postTransactionsResponseResponse.isSuccessful()) {
            throw new RuntimeException("failed to create smart contract transaction " +  postTransactionsResponseResponse.message());
        }

        String id = postTransactionsResponseResponse.body().txId;
        log.info("smart contract transaction created with id: {}", id);

        waitForConfirmation(id, transactionDefaultTimeout);

        PendingTransactionResponse pendingTransactionResponse = algodClient.PendingTransactionInformation(id).execute().body();
        pendingTransactionResponse.txn.transactionID = id;
        return pendingTransactionResponse;
    }

    private PendingTransactionResponse waitForConfirmation(String transactionId, Integer timeout)
            throws Exception {
        if(algodClient == null || transactionId == null || timeout < 0) {
            throw new IllegalArgumentException("Bad arguments for waitForConfirmation.");
        }
        Response<NodeStatusResponse> resp = algodClient.GetStatus().execute();
        if (!resp.isSuccessful()) {
            throw new Exception(resp.message());
        }
        NodeStatusResponse nodeStatusResponse = resp.body();
        Long startRound = nodeStatusResponse.lastRound+1;
        Long currentRound = startRound;
        while(currentRound < (startRound + timeout)) {
            Response<PendingTransactionResponse> resp2 = algodClient.PendingTransactionInformation(transactionId).execute();
            if (resp2.isSuccessful()) {
                PendingTransactionResponse pendingInfo = resp2.body();
                if (pendingInfo != null) {
                    if (pendingInfo.confirmedRound != null && pendingInfo.confirmedRound > 0) {
                        pendingInfo.txn.transactionID = transactionId;
                        return pendingInfo;
                    }
                    if (pendingInfo.poolError != null && pendingInfo.poolError.length() > 0) {
                        throw new Exception("The transaction has been rejected with a pool error: " + pendingInfo.poolError);
                    }
                }
            }

            Response<NodeStatusResponse> resp3 = algodClient.WaitForBlock(currentRound).execute();
            if (!resp3.isSuccessful()) {
                throw new Exception(resp3.message());
            }
            currentRound++;
        }
        throw new Exception("Transaction not confirmed after " + timeout + " rounds!");
    }
}
