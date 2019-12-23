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
- ```--listen[=8899]```: enable API for remote commands & GUI. Authentication on startup is optional, but you can authenticate on startup with --authenticate
- ```--mixs-target```: minimum number of mixs to achieve per UTXO
- ```--authenticate```: will ask for your passphrase at startup
- ```--list-pools```: list pools and exit


## Expert usage

#### API
whirlpool-client-cli can be managed remotely with REST API. See [README-API.md](README-API.md)


#### Debugging
- ```--debug```: debug logs
- ```--debug-client```: more debug logs
- ```--dump-payload```: dump pairing-payload of current wallet and exit


#### Authenticate on startup
You can authenticate in several ways:
- ```--authenticate```: manually type your passphrase on startup
- ```--listen```: use the GUI or API to authenticate remotely


For security reasons, you should not store your passphrase anywhere. If you really need to automate authentication process, use this at your own risk:
```
export PP="mypassphrase"
echo $PP|java -jar whirlpool-client-cli-x-run.jar --authenticate
```

#### Tor configuration
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


#### Configuration override
Configuration can be overriden in whirlpool-cli-config.properties (see default configuration in [src/main/resources/application.properties]).

Or with following arguments:
- ```--scode```: scode to use for tx0
- ```--tx0-max-outputs```: tx0 outputs limit
- ```--auto-tx0=[poolId]```: run tx0 from deposit utxos automatically
- ```--auto-mix=[true/false]```: mix premix utxos automatically


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

