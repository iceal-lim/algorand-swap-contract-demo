package cloud.blockchainsolutions.algorand.domain;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class Token {

    private Long assetId;

    private Long priceInMicroAlgo;

}
