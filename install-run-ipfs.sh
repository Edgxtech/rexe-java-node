#! /bin/sh
wget https://dist.ipfs.io/kubo/v0.20.0/kubo_v0.20.0_darwin-arm64.tar.gz -O /tmp/kubo_darwin-arm64.tar.gz
tar -xvf /tmp/kubo_darwin-amd64.tar.gz
#wget https://dist.ipfs.io/kubo/v0.18.1/kubo_v0.18.1_linux-amd64.tar.gz -O /tmp/kubo_linux-amd64.tar.gz
#wget https://dist.ipfs.io/kubo/v0.18.1/kubo_v0.18.1_darwin-arm64.tar.gz -O /tmp/kubo_v0.18.1_darwin-arm64.tar.gz
#tar -xvf /tmp/kubo_v0.18.1_darwin-arm64.tar.gz

export PATH=$PATH:$PWD/kubo/
ipfs init
ipfs daemon --routing=dhtserver &
sleep 10s
time ipfs pin add zdpuAwfJrGYtiGFDcSV3rDpaUrqCtQZRxMjdC6Eq9PNqLqTGg
