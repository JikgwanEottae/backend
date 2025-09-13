# 직관어때 - 야구 직관일기 기록 어플
> **야구 직관일기를 기록하고 오늘의 직관운세를 확인하는 서비스**  
> 
> 사용자는 야구장 근처 관광지 및 놀러갈만한 장소를 제공받을 수 있습니다.  
> 사용자는 KBO 경기 일정과 결과를 확인하고, 기록하며 직관 승률과 일기를 기록할 수 있습니다.  
> 사용자는 오늘의 직관 운세 정보를 제공받을 수 있습니다. 


---





## 시스템 아키텍처



<p align="center">
  <img src="https://github.com/user-attachments/assets/7b9e8a04-c33d-4dec-b488-a9ff31a3ee28" alt="직관어때 시스템 아키텍처" width="600"/>
</p>



---

## 🛠 **기술 스택**
### 🔹 백엔드
| 기술 | 버전 | 사용 목적 |
|------|------|-----------|
| **Java** | 17 | 메인 프로그래밍 언어 |
| **Spring Boot** | 3.5.3 | 애플리케이션 프레임워크 |
| **Spring Web (MVC)** | 3.5.x | REST API 제공 |
| **Spring WebFlux (WebClient)** | 3.5.x | 비동기 HTTP 클라이언트 |
| **Spring Data JPA** | - | ORM, 데이터 접근 계층 |
| **Bean Validation** | - | 요청 데이터 유효성 검증 |
| **Spring Security** | - | 인증/인가 프레임워크 |
| **OAuth2 Client** | - | 카카오/애플 로그인 연동 |
| **JWT (jjwt + nimbus-jose-jwt/ES256)** | 0.11.5 / 9.37 | 토큰 발급/검증 |

### 🔹 데이터 저장 및 처리
| 기술 | 버전 | 사용 목적 |
|------|------|-----------|
| **MySQL** | 8.0 | 관계형 데이터베이스 |
| **Redis** | 7 | 캐시(게임 일정 프리워밍 등) |
| **Spring Batch** | - | 배치 잡(사용자 정리 등) |
| **Selenium + WebDriverManager** | 4.8.0 / 5.3.2 | KBO 일정 크롤링 |
| **AWS SDK v2 (S3)** | 2.20.29 | 이미지 업로드/관리 |

### 🔹 인프라 및 배포
| 기술 | 버전 | 사용 목적 |
|------|------|-----------|
| **Docker** | - | 컨테이너화 |
| **docker-compose** | - | 로컬 멀티 컨테이너(app/db/redis/selenium) |
| **AWS CodeDeploy** | - | EC2 배포(appspec.yml + scripts) |
| **Gradle** | - | 빌드/의존성 관리 |
| **Springdoc OpenAPI (Swagger UI)** | 2.8.9 | API 문서화 |
| **Actuator** | - | 헬스체크/모니터링 |
| **dotenv-java** | 3.0.0 | 환경 변수 로딩 |

### 🔹 테스트 및 개발 도구
| 기술 | 버전 | 사용 목적 |
|------|------|-----------|
| **JUnit** | 5 | 단위/통합 테스트 |
| **Spring Boot Test** | - | 테스트 유틸리티 |
| **Lombok** | - | 보일러플레이트 제거 |
| **Git & GitHub** | - | 버전 관리 |

---


## 🏗 **프로젝트 구조 및 아키텍처**
### 🔹 패키지 구성
```
yagu
├─ common                  # 공통 모듈(설정/보안/예외/응답/JWT/OAuth)
│  ├─ config               # AppConfig, S3Config, SwaggerConfig, WebConfig 등
│  ├─ exception            # 전역 예외 및 에러 코드
│  ├─ jwt                  # 토큰 발급/검증, 필터, 리프레시 토큰
│  ├─ oauth                # 카카오/애플 OAuth 클라이언트 및 설정
│  ├─ response             # ApiResponse 래퍼
│  └─ security             # SecurityConfig, UserDetails 등
├─ community               # 커뮤니티(게시글/댓글/좋아요/이미지)
│  ├─ controller │ dto │ entity │ repository │ service
├─ diary                   # 직관 일기 및 사용자 통계
│  ├─ controller │ dto │ entity │ repository │ service
├─ game                    # KBO 경기/일정 및 크롤러, 캐시
│  ├─ cache                # 캘린더 캐시 서비스/스케줄러/워밍러너
│  ├─ controller │ dto │ entity │ repository
│  └─ GameScheduleCrawler.java
├─ image                   # 이미지 업로드(S3)
│  ├─ controller │ service
├─ saju                    # 직관 운세 연동(FastAPI)
│  ├─ config │ controller │ dto │ service
├─ tourapi                 # 야구장 주변 관광정보 연동
├─ user                    # 인증/프로필/배치
│  ├─ batch                # Spring Batch 잡 및 스케줄러
│  ├─ controller │ dto │ entity │ repository │ service
└─ YaguApplication.java    # 메인 진입점
```

### 🔹 레이어드 아키텍처
- **Controller**: REST 엔드포인트, DTO 변환
- **Service**: 비즈니스 로직, 트랜잭션 경계
- **Repository**: JPA 기반 영속성 계층
- **Cross-cutting**: 보안, 예외 처리, 응답 규격은 `common`에서 공통 제공

### 🔹 인증/인가
- OAuth2 로그인(카카오/애플) → 자체 JWT 발급
- JWT 필터 + `SecurityConfig`로 엔드포인트 보호, 리프레시 토큰은 DB 보관

### 🔹 데이터 & 캐시
- MySQL 8.0을 주 데이터 저장소로 사용
- Redis 7로 경기 캘린더 등 읽기 빈도 높은 데이터 캐시 및 프리워밍

### 🔹 배치/스케줄링
- Spring Batch로 사용자 삭제 정리 등 배치 처리
- 스케줄러로 게임 일정 캐시 워밍 및 배치 트리거

### 🔹 외부 연동
- Selenium 크롤러로 KBO 일정 수집
- WebClient로 FastAPI(사주), 공공 관광 API 연동
- AWS S3로 이미지 저장

### 🔹 운영/배포
- 멀티스테이지 Dockerfile로 경량 런타임 이미지
- AWS CodeDeploy로 EC2 배포(`appspec.yml` + `deploy/scripts`)
---
## 📞 **문의** 
yagudev22@gmail.com
