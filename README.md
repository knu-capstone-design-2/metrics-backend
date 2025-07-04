# metrics-backend

`metrics-backend`는 컨테이너 및 호스트 머신에서 메트릭 데이터를 수집하고, Kafka를 통해 전송 및 처리하는 백엔드 시스템입니다. 이 프로젝트는 메트릭 수집 → Kafka 전송 → 데이터 처리의 흐름을 중심으로 구성되며, Docker 환경에서 실행됩니다.

## 📁 모듈 구성

- **data-collector**  
  컨테이너 머신의 자원 사용 데이터를 수집합니다.

- **localhost-data-collector**  
  호스트 머신의 자원 사용 데이터를 수집합니다.

- **producer**  
  수집된 데이터를 Kafka로 전송하는 Kafka 프로듀서 역할을 합니다.

- **consumer**  
  Kafka로부터 메트릭 데이터를 수신하며, 내부적으로 WebClient나 WebSocket을 활용해 데이터를 외부에 전송하는 기능도 수행합니다.

---

## 📁 docker-compose 각 파일 설명

- **docker-compose.collector.yml**  
  메트릭을 수집하려는 컴퓨터들의 도커에 설치해 실행시킵니다.
  - metrics-backend/localhost-data-collector
  - metrics-backend/data-collector (의존성 존재:metrics-backend/producer)

- **docker-compose.backend.yml**  
  kafka로 받아온 메트릭을 처리하는 컴퓨터의 도커에 설치해 실행시킵니다.
  - metrics-backend/consumer
  - api-backend
  - MySQL 데이터베이스

---

## ⚙️ 실행 전 준비사항

- **Docker 설치**  
  이 프로젝트는 Docker 환경에서 동작하므로, 먼저 Docker가 설치되어 있어야 합니다.  
  👉 [Docker 설치 가이드](https://docs.docker.com/get-docker/)



---


## 🚀 실행 방법

```bash
# 실행 직전 디렉토리 구조
server-monitoring/
├── metric-backend/
├── api-backend/
├── docker-compose.backend.yml
└── docker-compose.collector.yml
```
#### 1. 폴더 생성
- 1-1. server-monitoring(이름 변경 가능) 폴더 안에 metric-backend와 api-backend를 git clone한다.

---

#### 2. 환경설정
- 2-1. metrics-backend의 각 모듈들과 api-backend에 환경설정(ex. env파일생성)을 해준다.
  - 환경설정은 아래의 **💻 환경설정** 부분을 참고하세요!

---

#### 3. Docker 실행 전 필수 준비 단계 (터미널 이용 권장)
- 3-1. (중요!) `metric-backend`폴더에 있는 **docker-compose.backend.yml**와 **docker-compose.collector.yml** 파일을 상위 폴더(server-monitoring)로 옮긴다.


- 3-2. `server-monitoring` 루트로 경로 이동.


- 3-3. DB 영구 저장을 위해 **최초 1회** 볼륨 생성해준다.
  - ※ 기존에 있는 DB를 써야한다면, DB설정은 아래의 **🏷️참고(api-backend env.properties 환경설정)** 를 참고하세요.
```bash
docker volume create mysql-db
```

- 3-4. 네트워크 **최초 1회** 생성해준다.
```bash
docker network create monitoring_network
```

---
#### 4. 각 docker-compose 실행 (터미널 이용 권장)
- 4-0. `server-monitoring` 루트에서 시도한다.

- 4-1. 백엔드 + DB + consumer 실행
```bash
docker-compose -f docker-compose.backend.yml up -d --build
```

- 4-2. collector 측 실행 (각 장비 or 서버컴 등에서)
```bash
docker-compose -f docker-compose.collector.yml up -d --build
```


- **순서대로 실행함을 강력히 권장합니다.**
- **테스트를 위해 하나의 컴퓨터에 `docker-compose.collector.yml`, `docker-compose.backend.yml`를 함께 실행시키는 것도 가능합니다.**


---

## 💻 환경설정
- 💡 주석으로 달아놓은 각 경로에 없는 폴더 및 파일이 없다면 **반드시** 새로 생성합니다.

```bash
# consumer/ ... /src/main/resources/properties/envfile.properties

GROUP_ID=[kafka consumer group id]
BOOTSTRAP_SERVER=[kafka 클러스터 ip주소:외부포트번호]
KAFKA_TOPIC_HOST=localhost
KAFKA_TOPIC_CONTAINER=container
KAFKA_GROUP_ID_STORAGE_GROUP=[kafka consumer group id]
API_BASE_URL=http://api-backend:8004
SOCKET_ALLOWED_ADDR=http://localhost:3000 (임시, 프론트 주소가 있다면 해당 경로로 변경)

# data-collector/ ... /src/main/resources/properties/envdc.properties
DATACOLLECTOR_BOOTSTRAP_SERVER=[kafka 클러스터 ip주소:외부포트번호]

# localhost-data-collector/ ... /src/main/resources/properties/envldc.properties
LOCALHOSTDATACOLLECTOR_BOOTSTRAP_SERVER=[kafka 클러스터 ip주소:외부포트번호]

# producer/ ... /src/main/resources/properties/envp.properties
PRODUCER_BOOTSTRAP_SERVER=[kafka 클러스터 ip주소:외부포트번호]

```
---

### 🏷️ 참고 (api-backend env.properties 환경설정)

```bash
# api-backend/ ... /src/main/resources/properties/env.properties
DATABASE_URL=jdbc:mysql://<엔드포인트>/monitoring_db
DATABASE_USERNAME=<Username>
DATABASE_PASSWORD=<Password>

cors.allowed-origins=<주소1>,<주소2>,...
```

```bash
# 테스트 시
# 도커에 임시 MySQL DB를 생성했을 때 env.properties 설정
DATABASE_URL=jdbc:mysql://mysql-db:3306/monitoring_db
DATABASE_USERNAME=monitoring_user
DATABASE_PASSWORD=monitoring_pass

cors.allowed-origins=http://localhost:3000
```

