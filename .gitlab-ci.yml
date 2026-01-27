stages:
  - build

build:
  stage: build
  image: eclipse-temurin:17-jdk
  cache:
    key: "$CI_PROJECT_ID"
    paths:
      - .gradle/wrapper
      - .gradle/caches
  script:
    - chmod +x gradlew
    - ./gradlew clean jarJar
  artifacts:
    name: "jars"
    paths:
      - build/libs/*.jar
    when: always
