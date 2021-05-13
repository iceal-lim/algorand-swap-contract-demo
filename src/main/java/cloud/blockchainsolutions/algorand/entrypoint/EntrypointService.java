package cloud.blockchainsolutions.algorand.entrypoint;

import cloud.blockchainsolutions.algorand.domain.ContractResponse;
import cloud.blockchainsolutions.algorand.domain.Token;
import cloud.blockchainsolutions.algorand.domain.TokenPurchaseRequest;
import cloud.blockchainsolutions.algorand.service.AccountService;
import cloud.blockchainsolutions.algorand.service.UserActionService;
import com.algorand.algosdk.account.Account;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EntrypointService {

    private final UserActionService userActionService;
    private final AccountService accountService;

    public EntrypointService(final UserActionService userActionService, final AccountService accountService) {
        this.userActionService = userActionService;
        this.accountService = accountService;
    }

    public void runDemo() throws Exception {
        log.info("EntrypointService.runDemo");

        // [Simulates] two accounts a buyer and a seller.
        // seller -- sells an ASA
        // buyer -- buys it via swap using a contract
        Account seller = accountService.getSellerAccount();
        Account buyer = accountService.getBuyerAccount();

        // [Simulates] buyer selecting a token in the website.
        Token token = userActionService.selectToken();

        // Amount of ASA tokens user wants to purchase
        final long numberOfTokensToPurchase = 1L;
        TokenPurchaseRequest tokenPurchaseRequest = userActionService.purchaseToken(token, buyer, numberOfTokensToPurchase);

        // userActionService.buyerAssetOptIn(token, buyer);

        // getting the string address of the users, the website does not ask for a user's keys (mnemonic), just address strings.
        // [WEBSITE: NO USER KEYS REQUIRED]
        String sellerAddress = seller.getAddress().toString();
        String buyerAddress = seller.getAddress().toString();

        // Once buyer is done creating the purchase request, a contract is created.
        // Buyer will receive a contract address, and the seller is notified and also sent a contract address.
        // [WEBSITE-TRIGGERED: NO USER KEYS REQUIRED]
        ContractResponse contractResponse = userActionService.createPurchaseContract(token, buyerAddress, sellerAddress);

        // [Simulates] buyer sending the purchase amount to the contract.
        final Long allowanceForFeesAndMaintainingBalance = 100000L * 3;
        final Long totalAmount = token.getPriceInMicroAlgo() * tokenPurchaseRequest.getNumberOfTokens() + allowanceForFeesAndMaintainingBalance;
        userActionService.buyerSendsAlgo(buyer, contractResponse.getAddress(), totalAmount, "purchase");

        // Once buyer is done funding the contract, contract can now perform transaction and opts-in. [WEBSITE-TRIGGERED: NO USER KEYS REQUIRED]
        userActionService.contractOptsInToAsset(token, contractResponse, buyerAddress, sellerAddress);

        // [Simulates] seller sending the purchase amount to the contract.
        userActionService.sellerSendsToken(token, seller, contractResponse.getAddress(), tokenPurchaseRequest.getNumberOfTokens());

        // The swap is executed. [WEBSITE-TRIGGERED: NO USER KEYS REQUIRED]
        userActionService.executeSwap(contractResponse, token, tokenPurchaseRequest, buyerAddress, sellerAddress);
    }
}
