package cloud.blockchainsolutions.algorand.service;

import com.algorand.algosdk.account.Account;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.GeneralSecurityException;

@Service
public class AccountService {

    @Value("${mnemonic.app}")
    private String appMnemonic;

    @Value("${mnemonic.buyer}")
    private String buyerMnemonic;

    @Value("${mnemonic.seller}")
    private String sellerMnemonic;

    public Account getAppAccount() throws GeneralSecurityException {
        return new Account(appMnemonic);
    }

    public Account getBuyerAccount() throws GeneralSecurityException {
        return new Account(buyerMnemonic);
    }

    public Account getSellerAccount() throws GeneralSecurityException {
        return new Account(sellerMnemonic);
    }
}
