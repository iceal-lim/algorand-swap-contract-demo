# algorand-swap-contract-demo

This project was created to report a problem, but since it has useful examples on different transaction, it's better to share.

**Warning: use this in test net only, using test-net ALGOS and ASAs**

What it does: Two users, a `buyer` and a `seller`, trying to swap a ALGOs for ASA token.

1. The `buyer` of the ASA will look for the list of tokens in a website and selects.
2. Once an ASA token is selected, the `buyer` will then create a purchase request, indicating the number of tokens.
3. The `buyer` needs to optIn to the asset using his/her wallet. 

    In `application.yaml`, a sample asset is set.
```
    token:
      asset: 15806118
```

4. A contract is created after the `buyer` triggers it from the website.
5. The contract address and QR code is presented to the `buyer` where the `buyer` will pay using his/her wallet.
6. Once the `buyer` pays, the `buyer` will then trigger the notification to the seller. During this step, the contract will then opt-in to the selected asset. It needs to be in this stage because the contract-account requires funds to perform the operation.
7. The `seller` of the asset receives the contract address and QR code.
8. The `seller` sends the asset to the contract address and then executes the contract through the website.
9. The swap happens, and any unspent ALGOs goes back to the `buyer`, and ASA token will go back to the `seller`. (In case that either one sent more than required values)


See `EntrypointService.java` where the steps above are orchestrated.


To compile

```
mvn clean install
```

To run

Buyer account (MNEMONIC_SELLER) needs to own an ASA with assetId specified in the application.yaml.

```
export MNEMONIC_SELLER="your 25 word mnemonic that is used for simulation"

export MNEMONIC_BUYER="your 25 word mnemonic that is used for simulation"

java -jar target/algorand-swap-contract-demo-0.0.1.jar

```

