# How to run example

## Requirements

List of required software:
* [SBT](https://www.scala-sbt.org/)
* [kubectl](https://kubernetes.io/docs/tasks/tools/install-kubectl/)
* [Minikube](https://kubernetes.io/docs/setup/learning-environment/minikube/)

## Steps

1. Start minikube cluster. If you want to run cluster with network policy functionality enabled: 
```shell script
$ minikube start --network-plugin=cni --enable-default-cni
```
or without network policy:
```shell script
$ minikube start
```
2. In the main directory type run command:
```shell script
$ export IAT_KUBE_CONTEXT='minikube'
$ sbt "examples/runMain com.virtuslab.iat.examples.GuestBook"
```
3. Inspect if all pods are in `Running` state:
```shell script
$ kubectl get po -n guestbook
```
4. Get frontend URL from minikube:
```shell script
$ minikube service -n guestbook frontend --url
```