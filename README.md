[![Build Status](https://travis-ci.org/Samourai-Wallet/whirlpool-client-cli.svg?branch=develop)](https://travis-ci.org/Samourai-Wallet/whirlpool-client-cli)
[![](https://jitpack.io/v/Samourai-Wallet/whirlpool-client-cli.svg)](https://jitpack.io/#Samourai-Wallet/whirlpool-client-cli)

# whirlpool-client-cli

Command line client for [Whirlpool](https://github.com/Samourai-Wallet/Whirlpool) by Samourai-Wallet.


## Setup
You can setup whirlpool-client-cli in 2 ways:
- commandline: run CLI with ```--init```
- remotely through API: run CLI with ```--listen```, then open GUI


## General usage
```
java -jar target/whirlpool-client-version-run.jar
[--listen[=8899]] [--authenticate]
[--server={MAINNET,TESTNET}] [--tor] [--proxy=(socks|http)://host:port] [--mixs-target=]
[--debug] [--debug-client] [--pushtx=auto|interactive|http://user:password@host:port] [--scode=] [--tx0-max-outputs=] {args...}
```

#### Optional arguments:
- listen: enable API for remote commands & GUI. Authentication on startup is optional, but you can authenticate on startup with --authenticate
- server: whirlpool server to connect to
- tor: route all traffic to TOR
- proxy: connect through SOCKS/HTTP proxy.
    Tor proxy: socks://localhost:9050
    Tor Browser: socks://localhost:9150
    HTTP proxy: http://your-proxy:8080
- mixs-target: number of mixs to achieve per UTXO

#### Tech arguments: you probably shouldn't use it
- debug: display debug logs from cli
- debug-client: display debug logs from whirlpool-client
- pushtx: specify how to broadcast transactions (tx0, aggregate).
    * auto: by default, tx are broadcasted through Samourai service.
    * interactive: print raw tx and pause to let you broadcast it manually.
    * http://user:password@host:port: rpc connection to your own bitcoin node (connection is not encrypted, use on trusted network only).
- scode: optional scode to use for tx0
- tx0-max-outputs: tx0 outputs limit

### List pools
```
--list-pools
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --list-pools
```

### Mix a wallet
You need a wallet holding funds to mix.

```
[--pool=] [--client-delay=5] [--tx0-delay=20]
[--auto-tx0=poolId] [--auto-mix] [--auto-aggregate-postmix]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar
```
- pool: poolId(s) to use, ordered by priority and separated by ','. ie: '0.1btc,0.01btc'. By default all pools will be used.
- client-delay: delay (in seconds) between each connexion
- tx0-delay: delay (in seconds) between each tx0 (from --auto-tx0)
- auto-tx0: automatically run tx0 from deposit for specified pool when premix wallet is empty
- auto-mix: automatically mix utxos detected in premix wallet
- auto-aggregate-postmix: enable automatically post-mix wallet agregation to refill premix when empty

## Expert usage


### Dump pairing payload of current wallet
```
--dump-payload
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --dump-payload
```


### Aggregate postmix / move funds
Move all postmix funds back to premix wallet and consolidate to a single UTXO.
Only allowed on testnet for testing purpose.
```
--aggregate-postmix[=address]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar --aggregate-postmix
```
- aggregate-postmix: move funds back to premix-wallet. Or --aggregate-postmix=address to move funds to a specific address.

### Configuration override
Local configuration can be overriden with:
```
--api-key=
```

### Expert configuration
```
cli.torConfig.executable = /path/to/bin/tor
```
- Use `auto` to use embedded tor, or detecting a local TOR install when your system is not supported.
- Use `local` to detect a local tor install.
- Use custom path to `tor` binary to use your own tor install.

### API
whirlpool-client-cli can be managed with a REST API. See [README-API.md](README-API.md)

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
