[![Build Status](https://travis-ci.org/Samourai-Wallet/whirlpool-client-cli.svg?branch=develop)](https://travis-ci.org/Samourai-Wallet/whirlpool-client-cli)
[![](https://jitpack.io/v/Samourai-Wallet/whirlpool-client-cli.svg)](https://jitpack.io/#Samourai-Wallet/whirlpool-client-cli)

# whirlpool-client-cli

Command line client for [Whirlpool](https://github.com/Samourai-Wallet/Whirlpool) by Samourai-Wallet.


## General usage
```
java -jar target/whirlpool-client-version-run.jar --server=host:port [--network=test{,main}] [--listen[=8899]]
[--ssl=true] [--tor=true] [--debug] [--scode=]
[--pushtx=auto|interactive|http://user:password@host:port] {args...}
```

### Required arguments:
- server: (host:port) server to connect to

### Optional arguments:
- network: (main,test) bitcoin network to use. Client will abort if server runs on a different network.
- listen: enable API for remote commands
- ssl: enable or disable SSL
- tor: enable or disable TOR
- debug: display more logs for debugging
- scode: optional scode to use for tx0
- pushtx: specify how to broadcast transactions (tx0, aggregate).
    * auto: by default, tx are broadcasted through Samourai service.
    * interactive: print raw tx and pause to let you broadcast it manually.
    * http://user:password@host:port: rpc connection to your own bitcoin node (connection is not encrypted, use on trusted network only).

### List pools
```
--server=host:port --list-pools
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --server=host:port --list-pools
```

### Mix a wallet
You need a wallet holding funds to mix. The script will run the following automatic process:
1. List wallet utxos
2. When needed, split existing wallet utxo to pre-mix utxos with a valid tx0. Broadcast it (when rpc-client-url provided) or halt to let you broadcast it manually.
3. Mix pre-mix utxos, and repeat

```
--server=host:port [--rpc-client-url=http://user:password@host:port]
[--clients=1] [--client-delay=5] [--auto-tx0] [--auto-mix] [--auto-aggregate-postmix]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --rpc-client-url=http://user:password@host:port
```
- clients: number of simultaneous mixs
- client-delay: delay (in seconds) between each connexion
- auto-tx0: automatically run tx0 from deposit when premix wallet is empty
- auto-mix: automatically mix utxos detected in premix wallet
- auto-aggregate-postmix: enable automatically post-mix wallet agregation to refill premix when empty

## Expert usage

### Mix specific utxo
You need a valid pre-mix utxo (output of a valid tx0) to mix.
```
--server=host:port --pool=
--utxo= --utxo-key= --utxo-balance=
[--mixs=1]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --utxo=5369dfb71b36ed2b91ca43f388b869e617558165e4f8306b80857d88bdd624f2-3 --utxo-key=cN27hV14EEjmwVowfzoeZ9hUGwJDxspuT7N4bQDz651LKmqMUdVs --utxo-balance=100001000
```
- pool: id of the pool to join
- utxo: (txid:ouput-index) pre-mix input to spend (obtained from a valid tx0)
- utxo-key: ECKey for pre-mix input
- utxo-balance: pre-mix input balance (in satoshis). Whole utxo-balance balance will be spent.
- mixs: (1 to N) number of mixes to complete. Client will keep running until completing this number of mixes.


### Aggregate postmix / move funds
Move all postmix funds back to premix wallet and consolidate to a single UTXO.
Only allowed on testnet for testing purpose.
```
--server=host:port [--rpc-client-url=http://user:password@host:port] --pool=
--aggregate-postmix[=address]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --aggregate-postmix --rpc-client-url=http://user:password@host:port
```
- aggregate-postmix: move funds back to premix-wallet. Or --aggregate-postmix=address to move funds to a specific address.

### API
whirlpool-client-cli can be managed with a REST API. See [README-API.md]

## Build instructions
Build with maven:

```
cd whirlpool-client-cli
mvn clean install -Dmaven.test.skip=true
```

## Resources
 * [whirlpool](https://github.com/Samourai-Wallet/Whirlpool)
 * [whirlpool-protocol](https://github.com/Samourai-Wallet/whirlpool-protocol)
 * [whirlpool-client](https://github.com/Samourai-Wallet/whirlpool-client)
 * [whirlpool-server](https://github.com/Samourai-Wallet/whirlpool-server)
