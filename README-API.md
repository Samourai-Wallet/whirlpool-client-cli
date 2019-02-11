# whirlpool-client-cli API

whirlpool-client-cli exposes a REST API when started with --listen[=port].
Default port: 8899

## Wallet

### Wallet utxos: ```GET /rest/wallet/utxos```
```
{
    deposit: {
        utxos: [],
        balance: 0
    },
    premix: {
        utxos: [],
        balance: 0
    },
    postmix: {
        utxos: [],
        balance: 0
    }
}
```

### Deposit: ```GET /rest/wallet/deposit[?increment=false]```
Parameters:
* Use increment=true make sure this address won't be reused.
```
{
    depositAddress: "tb1qjxzp9z2ax8mg9820dvwasy2qtle4v2q6s0cant"
}

## Mix

### Mix state: ```GET /rest/mix```
```
{

    "started":true,
    "nbMixing":1,
    "maxClients":10,
    "nbIdle":6,
    "nbQueued":17,
    "threads":[{
        "hash":"c7f456d5ff002faa89dadec01cc5eb98bb00fdefb92031890324ec127f9d1541",
        "index":5,
        "value":1000121,
        "confirmations":95,
        "path":"M/0/166",
        "account":"PREMIX",
        "status":"MIX_STARTED",
        "poolId":"0.01btc",
        "priority":5,
        "mixsTarget":1,
        "mixsDone":0,
        "message":" - [MIX 1/1] ▮▮▮▮▮▯▯▯▯▯ (5/10) CONFIRMED_INPUT : joined a mix!",
        "error":null
    }]
}
```
