# whirlpool-client-cli

Command line client for [Whirlpool](https://github.com/Samourai-Wallet/Whirlpool) by Samourai-Wallet.


## General usage
```
java -jar target/whirlpool-client-version-run.jar --network={main,test} [--server=host:port] [--debug] [--pool=] [--test-mode] {args...}
```
- network: (main,test) bitcoin network to use. Client will abort if server runs on a different network.
- server: (host:port) server to connect to
- debug: display more logs for debugging
- pool: id of the pool to join
- test-mode: disable tx0 checks, only available when enabled on server

### List pools
```
--network={main,test} [--server=host:port] [--debug]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc
```

### Mix a wallet
You need a wallet holding funds to mix. The script will run the following automatic process:
1. List wallet utxos
2. When needed, split existing wallet utxo to pre-mix utxos with a valid tx0. Broadcast it (when rpc-client-url provided) or halt to let you broadcast it manually.
3. Mix pre-mix utxos, and repeat

```
--network={main,test} [--server=host:port] [--debug] [--test-mode] --pool=
--seed-passphrase= --seed-words=
[--iteration-delay=0] [--client-delay=0]
[--rpc-client-url=http://user:password@host:port]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --seed-passphrase=foo --seed-words="all all all all all all all all all all all all --tx0=10"
```
- seed-passphrase & seed-words: wallet seed
- iteration-delay: delay (in seconds) to wait between mixs
- client-delay: delay (in seconds) between each client connexion
- rpc-client-url: rpc url to connect to your own bitcoin node for broadcasting tx0 transactions (warning: connection is not encrypted, use on trusted network only). If not provided, client will stop to let you broadcast it manually.

## Expert usage

### Mix specific utxo
You need a valid pre-mix utxo (output of a valid tx0) to mix.
```
--network={main,test} [--server=host:port] [--debug] [--test-mode] --pool=
--utxo= --utxo-key= --utxo-balance=
--seed-passphrase= --seed-words= [--paynym-index=0]
[--mixs=1]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --utxo=5369dfb71b36ed2b91ca43f388b869e617558165e4f8306b80857d88bdd624f2-3 --utxo-key=cN27hV14EEjmwVowfzoeZ9hUGwJDxspuT7N4bQDz651LKmqMUdVs --utxo-balance=100001000 --seed-passphrase=foo --seed-words="all all all all all all all all all all all all --paynym-index=5"
```
- utxo: (txid:ouput-index) pre-mix input to spend (obtained from a valid tx0)
- utxo-key: ECKey for pre-mix input
- utxo-balance: pre-mix input balance (in satoshis). Whole utxo-balance balance will be spent.
- seed-passphrase & seed-words: wallet seed from which to derive the paynym for computing post-mix address to receive the funds
- paynym-index: paynym index to use for computing post-mix address to receive the funds
- mixs: (1 to N) number of mixes to complete. Client will keep running until completing this number of mixes.


### Tx0
You need a wallet holding funds to split.
```
--network={main,test} [--server=host:port] [--debug] [--test-mode] --pool=
--seed-passphrase= --seed-words=
[--rpc-client-url=http://user:password@host:port]
--tx0=
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --seed-passphrase=foo --seed-words="all all all all all all all all all all all all --tx0=10"
```
- seed-passphrase & seed-words: wallet seed
- rpc-client-url: rpc url to connect to your own bitcoin node for broadcasting tx0 transactions (warning: connection is not encrypted, use on trusted network only). If not provided, client will stop to let you broadcast it manually.
- tx0: number of pre-mix utxo to generate

### Aggregate postmix
Move all postmix funds back to premix wallet and consolidate to a single UTXO.
Only allowed on testnet for testing purpose.
```
--network={main,test} [--server=host:port] [--debug] [--test-mode] --pool=
--seed-passphrase= --seed-words=
[--rpc-client-url=http://user:password@host:port]
--aggregate-postmix
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --network=test --server=host:port --pool=0.1btc --seed-passphrase=foo --seed-words="all all all all all all all all all all all all --aggregate-postmix"
```
- seed-passphrase & seed-words: wallet seed
- rpc-client-url: rpc url to connect to your own bitcoin node for broadcasting tx0 transactions (warning: connection is not encrypted, use on trusted network only). If not provided, client will stop to let you broadcast it manually.


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
