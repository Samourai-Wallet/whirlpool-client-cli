package com.samourai.rpc.client;

import com.samourai.whirlpool.client.wallet.pushTx.AbstractPushTxService;
import java.lang.invoke.MethodHandles;
import java.net.URL;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import wf.bitcoin.javabitcoindrpcclient.BitcoinJSONRPCClient;
import wf.bitcoin.javabitcoindrpcclient.BitcoindRpcClient;

public class JSONRpcClientServiceImpl extends AbstractPushTxService implements RpcClientService {
  private static final Logger log = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  private BitcoinJSONRPCClient rpcClient;
  private String expectedChain;

  private static final String CHAIN_TESTNET = "test";
  private static final String CHAIN_MAINNET = "main";

  public JSONRpcClientServiceImpl(String rpcClientUrl, boolean isTestnet) throws Exception {
    log.info("Instanciating JSONRpcClientServiceImpl");
    this.expectedChain = isTestnet ? CHAIN_TESTNET : CHAIN_MAINNET;

    URL url = new URL(rpcClientUrl);
    this.rpcClient = new BitcoinJSONRPCClient(url);
  }

  @Override
  public boolean testConnectivity() {
    String nodeUrl = rpcClient.rpcURL.toString();
    log.info("Connecting to bitcoin node... url=" + nodeUrl);
    try {
      // verify node connectivity
      long blockHeight = rpcClient.getBlockCount();

      // verify node network
      if (!rpcClient.getBlockChainInfo().chain().equals(expectedChain)) {
        log.error(
            "Invalid chain for bitcoin node: url="
                + nodeUrl
                + ", chain="
                + rpcClient.getBlockChainInfo().chain()
                + ", expectedChain="
                + expectedChain);
        return false;
      }

      // verify blockHeight
      if (blockHeight <= 0) {
        log.error(
            "Invalid blockHeight for bitcoin node: url="
                + nodeUrl
                + ", chain="
                + rpcClient.getBlockChainInfo().chain()
                + ", blockHeight="
                + blockHeight);
        return false;
      }
      log.info(
          "Connected to bitcoin node: url="
              + nodeUrl
              + ", chain="
              + rpcClient.getBlockChainInfo().chain()
              + ", blockHeight="
              + blockHeight);
      return true;
    } catch (Exception e) {
      log.info("Error connecting to bitcoin node: url=" + nodeUrl + ", error=" + e.getMessage());
      return false;
    }
  }

  @Override
  public Optional<RpcRawTransactionResponse> getRawTransaction(String txid) {
    try {
      BitcoindRpcClient.RawTransaction rawTx = rpcClient.getRawTransaction(txid);
      if (rawTx == null) {
        return Optional.empty();
      }
      RpcRawTransactionResponse rpcTxResponse =
          new RpcRawTransactionResponse(rawTx.hex(), rawTx.confirmations());
      return Optional.of(rpcTxResponse);
    } catch (Exception e) {
      log.error("getRawTransaction error", e);
      return Optional.empty();
    }
  }

  @Override
  public void pushTx(String txHex) throws Exception {
    if (log.isDebugEnabled()) {
      log.debug("pushTx... " + txHex);
    } else {
      log.info("pushTx tx..." + txHex);
    }
    try {
      rpcClient.sendRawTransaction(txHex);
    } catch (Exception e) {
      log.error("Unable to broadcast tx: " + txHex, e);
      throw new Exception("Unable to broadcast tx: " + txHex);
    }
  }
}
