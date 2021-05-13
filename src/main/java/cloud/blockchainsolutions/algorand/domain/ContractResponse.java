package cloud.blockchainsolutions.algorand.domain;

import com.algorand.algosdk.crypto.LogicsigSignature;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ContractResponse {

    private LogicsigSignature logicsigSignature;
    private Long round;
    private String address;

}
