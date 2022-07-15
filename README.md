= Build

* After the cloning repository, go to the folder with the just cloned project
* Run build command: 
```bash
mvn clean install
```
* run docker image build:
```bash
docker build -t inventory-service:latest .
```
* go to .infra folder
```bash
cd .infra
```
* run docker-compose:
```bash
docker-compose up -d
```
the above command will run mongodb and spring boot app on 8080 port

After this application is ready to accept requests

There is a postman collection in the postman_collection folder