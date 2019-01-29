# whirlpool-client-cli API

whirlpool-client-cli exposes a REST API when started with --listen.


## Wallet

### Wallet status: ```GET /rest/wallet```
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
