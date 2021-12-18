# light-cd Operator

This is a cloud native simple deployment tool written in java, helps to deploy SPAship easily into any Kubernetes cluster, by using rest api

## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus now ships with a Dev UI, which is available in dev mode only at http://localhost:8080/q/dev/.

## Packaging and running the application

The application can be packaged using:

```shell script
./mvnw package
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory. Be aware that it’s not an _über-jar_ as
the dependencies are copied into the `target/quarkus-app/lib/` directory.

If you want to build an _über-jar_, execute the following command:

```shell script
./mvnw package -Dquarkus.package.type=uber-jar
```

The application is now runnable using `java -jar target/quarkus-app/quarkus-run.jar`.

## How to setup

### prerequisites
1. command-line tool such as kubectl to control kubernetes cluster.
2. helm tool
3. an existing kuernetes cluster

### steps
1. clone the repository into your local machine 
 ```shell script
   git clone https://github.com/arkaprovob/light-cd.git -b spaship
```
2. open a console and navigate to the root directory of the project
3. select namespace of the target kubernetes cluster and
 ```shell script
   kubectl config set-context --current --namespace=<namespace>
```
if you are on openshift then
 ```shell script
   oc project <project-name>
```
3. create a configuration map named `env-params` into the namespace which will contain the required secrets (environment specific) to run the spaship setup.To know more follow the **##env-params** section below
4. Apply the helm command
 ```shell script
 helm install --set image.repository=quay.io/arbhatta/light-cd --set image.tag=spaship --set app.namespace=light-cd --set app.environment=* --set app.storageclass="<name-of-the-storage-class>" --set app.uimemory=1024Mi --set app.domain="<set-a-domain-for-ingress-controller>" --set app.instance="<name-of the-app-instance>" light-cd .
```
### env-params

a sample configuration map is available under light-cd/src/main/resources/openshift folder named `env-params.yaml` from where you will get an idea on the env variables which is required to setup spaship. You can add more config values over there. If you look closely the keys of the configmaps are the same as some of the  config placeholder in spaship-express-template.yaml, while setting up SPAship services under a namespace, `light-cd` app will pull those env specific config values from `env-params` configmap and set them accordingly.