# whirlpool-client-cli API

whirlpool-client-cli exposes a REST API when started with --listen[=8899].


### List pools: ```GET /rest/pools```
```
{
    "pools":[
        {
            "poolId":"0.1btc",
            "denomination":10000000,
            "feeValue":5000000,
            "mustMixBalanceMin":10000102,
            "mustMixBalanceMax":10010000,
            "minAnonymitySet":5,
            "nbRegistered":0,
            "mixAnonymitySet":5,
            "mixStatus":"CONFIRM_INPUT",
            "elapsedTime":22850502,
            "nbConfirmed":0,
            "tx0BalanceMin":10020005
        }
    ]
}
```

## Wallet

### Deposit: ```GET /rest/wallet/deposit[?increment=false]```
Parameters:
* (optional) Use increment=true make sure this address won't be reused.
```
{
    depositAddress: "tb1qjxzp9z2ax8mg9820dvwasy2qtle4v2q6s0cant"
}
```

## Global mix control

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
        "progressPercent":"10",
        "progressLabel":"CONNECTING",
        "poolId":"0.01btc",
        "priority":5,
        "mixsTarget":1,
        "mixsDone":0,
        "message":" - [MIX 1/1] ▮▮▮▮▮▯▯▯▯▯ (5/10) CONFIRMED_INPUT : joined a mix!",
        "error":null,
        "lastActivityElapsed": 23001
    }]
}
```

### Start mixing: ```POST /rest/mix/start```

### Stop mixing: ```POST /rest/mix/stop```

## UTXO controls

### List utxos: ```GET /rest/utxos```
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

### Tx0 ```POST /rest/utxos/{hash}:{index}/tx0```
Parameters:
```
{
    hash: "1758d42d5e0623dde9d4cbfacb89e4f914b97490889ec8b69a551caf5347face",
    index: 2,
    poolId: "0.01btc",
    mixsTarget: 0
}
```
* hash, index: utxo to spend for tx0
* poolId: id of pool to join
* mixsTarget: mixs limit (0 for unlimited)

```
{
    "txid":"aa079c0323349f4abf3fb793bf2ed1ce1e11c53cd22aeced3554872033bfa722"
}
```

### Start mixing UTXO: ```POST /rest/utxos/{hash}:{index}/startMix```
Parameters:
* hash,index: utxo to mix.

### Stop mixing UTXO: ```POST /rest/utxos/{hash}:{index}/stopMix```
Parameters:
* hash,index: utxo to stop mixing.
