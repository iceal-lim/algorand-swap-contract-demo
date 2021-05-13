from pyteal import *


def asset_swap_v2(owner, buyer, assetId, round, fee):
    print('creating asset_swap_v2 contract')
    # Use Cases

    # 1. Opt-In Contract
    opt_in = And(
        Global.group_size() == Int(1),
        Gtxn[0].fee() < Int(fee),
        Gtxn[0].group_index() == Int(0),
        Gtxn[0].type_enum() == TxnType.AssetTransfer,
        Gtxn[0].xfer_asset() == Int(assetId),
        Gtxn[0].asset_amount() == Int(0),
    )

    # 2. Asset Swap
    transfer_asset = And(
        Gtxn[0].type_enum() == TxnType.AssetTransfer,
        Gtxn[0].xfer_asset() == Int(assetId),
        Gtxn[0].asset_receiver() == Addr(buyer)
    )

    receive_payment = And(
        Gtxn[1].type_enum() == TxnType.Payment,
        Gtxn[1].receiver() == Addr(owner)
    )

    # 3. Rollback Transaction

    # cancel_transfers = And(
    #     Global.group_size() == Int(2),
    #     Gtxn[0].type_enum() == TxnType.Payment,
    #     Gtxn[0].receiver() == Addr(buyer),
    #     Gtxn[0].amount() == Int(0),
    #     Gtxn[0].close_remainder_to() == Addr(buyer),
    #
    #     Gtxn[1].type_enum() == TxnType.AssetTransfer,
    #     Gtxn[1].xfer_asset() == Int(assetId),
    #     Gtxn[1].asset_amount() == Int(0),
    #     Gtxn[1].asset_receiver() == Addr(owner)
    # )

    core = And(
        Txn.first_valid() > Int(round),
        Txn.fee() < Int(fee),
        Txn.rekey_to() == Global.zero_address()
    )

    core_transfer = And(
        Txn.first_valid() > Int(round),
        Global.group_size() == Int(2)
    )

    transfer = And(
        core_transfer,
        If(Txn.group_index() == Int(1), receive_payment, transfer_asset)
    )

    contract = And(
        core,
        If(Global.group_size() == Int(2), transfer, opt_in),
    )

    return contract


if __name__ == "__main__":
    owner = "OOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO"
    buyer = "BBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBBB"
    assetId = 999999
    round = 888888

    with open('/Users/USER/Desktop/asset_swap_v2.teal', 'w') as f:
        program = asset_swap_v2(owner, buyer, assetId, round, 2000)
        compiled = compileTeal(program, Mode.Signature, version=3)
        f.write(compiled)
        f.close()
