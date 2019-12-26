[![Build Status](https://travis-ci.org/Samourai-Wallet/whirlpool-client-cli.svg?branch=develop)](https://travis-ci.org/Samourai-Wallet/whirlpool-client-cli)
[![](https://jitpack.io/v/Samourai-Wallet/whirlpool-client-cli.svg)](https://jitpack.io/#Samourai-Wallet/whirlpool-client-cli)

# whirlpool-client-cli

Command line client for [Whirlpool](https://github.com/Samourai-Wallet/Whirlpool) by Samourai-Wallet.

## Getting started

#### Download and verify CLI
- Download whirlpool-client-cli-\[version\]-run.jar from [releases](https://github.com/Samourai-Wallet/whirlpool-client-cli/releases)
- Verify sha256 hash of the jar with signed message in whirlpool-client-cli-\[version\]-run.jar.sig
- Verify signature with [@SamouraiDev](https://github.com/SamouraiDev) 's key

#### Initial setup
You can setup whirlpool-client-cli in 2 ways:
- command-line: run CLI with ```--init```
- remotely: run CLI with ```--listen```, then use GUI or API

#### Run
```
java -jar target/whirlpool-client-version-run.jar
```

Optional arguments:
- ```--listen```: enable API for remote commands & GUI. Authentication on startup is optional, but you can authenticate on startup with --authenticate
- ```--mixs-target```: minimum number of mixs to achieve per UTXO
- ```--authenticate```: will ask for your passphrase at startup
- ```--list-pools```: list pools and exit


#### API
whirlpool-client-cli can be managed remotely with REST API. See [README-API.md](README-API.md)


#### Advanced usage
See [README-EXPERT.md](README-EXPERT.md) for advanced usage, integration and development.


## Resources
 * [whirlpool](https://github.com/Samourai-Wallet/Whirlpool)
 * [whirlpool-protocol](https://github.com/Samourai-Wallet/whirlpool-protocol)
 * [whirlpool-client](https://github.com/Samourai-Wallet/whirlpool-client)
 * [whirlpool-server](https://github.com/Samourai-Wallet/whirlpool-server)

