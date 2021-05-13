package cloud.blockchainsolutions.algorand.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TokenPurchaseRequest {

    private Token token;

    private String buyerAddress;

    private String contractAddress;

    private Long numberOfTokens;

    private Long minRound;

}
