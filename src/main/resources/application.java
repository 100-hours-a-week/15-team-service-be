/*server:
  port: 8080

spring:
  application:
    name: CommitMe

  profiles:
    include: local



 # datasource:
 #   driver-class-name: com.mysql.cj.jdbc.Driver
 #   url: jdbc:mysql://127.0.0.1:3306/resume?useSSL=false&allowPublicKeyRetrieval=true&useUnicode=true&characterEncoding=UTF-8&serverTimezone=Asia/Seoul
 #   username: root
 #   password: 220820

  jpa:
    hibernate:
      ddl-auto: update
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
        show_sql: true
        jdbc:
          time_zone: Asia/Seoul


#security:
#  jwt:
#    secret: "change-this-to-a-very-long-random-string-at-least-32-characters"
#    access-expiration: 30m
#    refresh-expiration: 14d

#  crypto:
#    access-token-key: "NNheHq6fZBn8WyV/tAWeLfqE1vJfUK8LcWmEDeeuvao="

logging:
  level:
    org.hibernate.SQL: debug
    org.hibernate.orm.jdbc.bind: trace

management:
  endpoints:
    web:
      exposure:
        include: health

*/