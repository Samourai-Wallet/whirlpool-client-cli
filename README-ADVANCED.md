# whirlpool-client-cli for advanced users


## Advanced usage

#### Debugging
- ```--debug```: debug logs
- ```--debug-client```: more debug logs
- ```--dump-payload```: dump pairing-payload of current wallet and exit

Any problem with a remote CLI? Test it locally:
- Configure CLI manually: ```java -jar whirlpool-client-cli-xxx-run.jar --debug --init```
- Then start it with manual authentication: ```java -jar whirlpool-client-cli-xxx-run.jar --debug --authenticate```

#### Log file
You can configure a log file in whirlpool-cli-config.properties:
```
logging.file = /tmp/whirlpool-cli.log
```

#### Testing loop
You can run CLI in loop mode on testnet to generate liquidity on testnet server:
- run TX0 while possible
- mix while possible
- consolidate wallet when PREMIX is empty and start again
```
--clients=5 --auto-tx0=0.01btc --tx0-max-outputs=15 --mixs-target=100 --scode=
```

Adjust mixing rate with ```cli.mix.clientDelay = 60```
Generate simultaneous liquidity with ```cli.mix.clientsPerPool = 5```



## Whirlpool integration


#### Authenticate on startup
You can authenticate in several ways:
- ```--authenticate```: manually type your passphrase on startup
- ```--listen```: use the GUI or API to authenticate remotely


For security reasons, you should not store your passphrase anywhere. If you really need to automate authentication process, use this at your own risk:
```
export PP="mypassphrase"
echo $PP|java -jar whirlpool-client-cli-x-run.jar --authenticate
```


#### Configuration override
Configuration can be overriden in whirlpool-cli-config.properties (see default configuration in [src/main/resources/application.properties]).

Or with following arguments:
- ```--scode=```: scode to use for tx0
- ```--tx0-max-outputs=```: tx0 outputs limit
- ```--auto-tx0=[poolId]```: run tx0 from deposit utxos automatically
- ```--auto-mix=[true/false]```: mix premix utxos automatically


#### Custom Tor configuration
Tor should be automatically detected, installed or configured. You can customize it for your needs:
```
cli.torConfig.executable = /path/to/bin/tor
```
- Use `auto` to use embedded tor, or detecting a local Tor install when your system is not supported.
- Use `local` to detect a local tor install.
- Use custom path to `tor` binary to use your own tor build.

Custom config can be appended to Torrc with:
```
cli.torConfig.customTorrc = /path/to/torrc
```

Tor mode can be customized with:
```
cli.torConfig.onionServer = true   # whirlpool server
cli.torConfig.onionBackend = true  # wallet backend
```
- `true`: Tor hidden services 
- `false`: clearnet over Tor


## Build instructions
Build with maven:

```
cd whirlpool-client-cli
mvn clean install -Dmaven.test.skip=true
```