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
[--mixs-target=]
[--debug] [--debug-client] [--scode=] [--tx0-max-outputs=] {args...}
```

#### Optional arguments:
- listen: enable API for remote commands & GUI. Authentication on startup is optional, but you can authenticate on startup with --authenticate
- mixs-target: minimum number of mixs to achieve per UTXO

#### Tech arguments: you probably shouldn't use it
- debug: display debug logs from cli
- debug-client: display debug logs from whirlpool-client
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
[--client-delay=5] [--tx0-delay=20]
[--auto-tx0=poolId] [--auto-mix] [--auto-aggregate-postmix]
```

Example:
```
java -jar target/whirlpool-client-version-run.jar
```
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



## Advanced configuration

### Tor
```
cli.torConfig.executable = /path/to/bin/tor
```
- Use `auto` to use embedded tor, or detecting a local Tor install when your system is not supported.
- Use `local` to detect a local tor install.
- Use custom path to `tor` binary to use your own tor build.

```
cli.torConfig.onionServer = true
cli.torConfig.onionBackend = true
```
When tor enabled, connect to whirlpool server or wallet backend through:
- `true`: Tor hidden services 
- `false`: clearnet over Tor


## API usage
whirlpool-client-cli can be managed with a REST API. See [README-API.md](README-API.md)

ApiKey can be overriden with:
```
--api-key=
```

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
