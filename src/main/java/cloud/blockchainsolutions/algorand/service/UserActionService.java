package cloud.blockchainsolutions.algorand.service;

import cloud.blockchainsolutions.algorand.domain.ContractResponse;
import cloud.blockchainsolutions.algorand.domain.Token;
import cloud.blockchainsolutions.algorand.domain.TokenPurchaseRequest;
import com.algorand.algosdk.account.Account;
import com.algorand.algosdk.v2.client.model.PendingTransactionResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

@Service
@Slf4j
public class UserActionService {

    @Value("${token.asset}")
    private Long assetId;

    private final AlgorandClientService algorandClientService;

    public UserActionService(final AlgorandClientService algorandClientService) {
        this.algorandClientService = algorandClientService;
    }

    public Token selectToken() {
        return Token.builder()
                .assetId(assetId)
                .priceInMicroAlgo(1000000L).build();
    }

    public TokenPurchaseRequest purchaseToken(Token token, Account buyerAccount, long amount) {
        return TokenPurchaseRequest.builder()
                .token(token)
                .buyerAddress(buyerAccount.getAddress().toString())
                .numberOfTokens(amount)
                .build();
    }

    public void buyerAssetOptIn(Token token, Account buyerAccount) throws Exception {
        PendingTransactionResponse response = algorandClientService.optInASA(token, buyerAccount);
        algorandClientService.printTransactionResponse(response);
    }

    public ContractResponse createPurchaseContract(Token token, String buyerAddress, String sellerAddress) throws Exception {
        return algorandClientService.createTokenPurchaseContract(token, buyerAddress, sellerAddress);
    }

    public void buyerSendsAlgo(Account buyerAccount, String contractAddress, Long microAlgos, String note) throws Exception {
        PendingTransactionResponse response = algorandClientService.sendAlgoToAddress(buyerAccount, contractAddress, microAlgos, note);
        algorandClientService.printTransactionResponse(response);
    }

    public void contractOptsInToAsset(Token token, ContractResponse contractResponse, String buyerAddress, String sellerAddress) throws Exception {
        PendingTransactionResponse response = algorandClientService.contractOptInASA(contractResponse, token, buyerAddress, sellerAddress);
        algorandClientService.printTransactionResponse(response);

    }

    public void sellerSendsToken(Token token, Account sellerAccount, String contractAddress, long numberOfTokens) throws Exception {
        PendingTransactionResponse response = algorandClientService.sendTokenToAddress(sellerAccount, contractAddress,numberOfTokens, token.getAssetId());
        algorandClientService.printTransactionResponse(response);
    }

    public void executeSwap(ContractResponse contractResponse, Token token, TokenPurchaseRequest purchaseRequest,
                            String buyerAddress, String sellerAddress) throws Exception {
        PendingTransactionResponse response = algorandClientService.executeSwapContract(contractResponse, token, purchaseRequest, buyerAddress, sellerAddress);
        algorandClientService.printTransactionResponse(response);
    }
}
