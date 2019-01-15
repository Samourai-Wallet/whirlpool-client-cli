package com.samourai.rpc.client;

import com.samourai.whirlpool.client.wallet.pushTx.PushTxService;
import java.util.Optional;

public interface RpcClientService extends PushTxService {
  Optional<RpcRawTransactionResponse> getRawTransaction(String txid);
}
