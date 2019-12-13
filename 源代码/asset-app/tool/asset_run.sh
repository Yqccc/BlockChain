#!/bin/bash 

function usage() 
{
    echo " Usage : "
    echo "   bash asset_run.sh deploy"
    echo "   bash asset_run.sh getCredit account"
    echo "   bash asset_run.sh getBill bill"
    echo "   bash asset_run.sh registerAccount account credit"
    echo "   bash asset_run.sh createBill bill from_account to_account amount ddl"
    echo "   bash asset_run.sh transferBill from_bill new_bill new_account amount"
    echo "   bash asset_run.sh financing from_bill new_bill amount"
    echo "   bash asset_run.sh payBill bill time"
    exit 0
}

    case $1 in
    deploy)
            [ $# -lt 1 ] && { usage; }
            ;;
    getCredit)
            [ $# -lt 2 ] && { usage; }
            ;;
    getBill)
            [ $# -lt 2 ] && { usage; }
            ;;
    registerAccount)
            [ $# -lt 3 ] && { usage; }
            ;;
    createBill)
            [ $# -lt 6 ] && { usage; }
            ;;
    transferBill)
            [ $# -lt 5 ] && { usage; }
            ;;
    financing)
            [ $# -lt 4 ] && { usage; }
            ;;
    payBill)
            [ $# -lt 3 ] && { usage; }
            ;;
    *)
        usage
            ;;
    esac

    java -Djdk.tls.namedGroups="secp256k1" -cp 'apps/*:conf/:lib/*' org.fisco.bcos.asset.client.AssetClient $@

