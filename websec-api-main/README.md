# websec-api

Spring Boot REST API za WebSec aplikaciju.

## Grana: vuln/jwt-verification

### Problem

Na grani vuln/jwt-verification class repoa, metoda extractAllClaims u JwtService
radila je samo base64 dekodiranje payloada bez ikakve kriptografske verifikacije potpisa:

  String payloadJson = new String(Base64.getDecoder().decode(parts[1]));
  Map<String, Object> map = mapper.readValue(payloadJson, Map.class);
  return Jwts.claims(map);  // potpis se nikad ne proverava

Napadac je mogao da:
1. Uzme validan token i dekoduje payload
2. Promeni "sub" polje (email) u email drugog korisnika
3. Potpise novim, proizvoljnim secret-om (ili ga ostavi bez potpisa)
4. Server prihvata takav token kao legitiman

### Resenje

Metoda extractAllClaims zamenjena sa:

  return Jwts.parserBuilder()
          .setSigningKey(getSignInKey())
          .build()
          .parseClaimsJws(token)  // kriptografski verifikuje HMAC-SHA256 potpis
          .getBody();

jjwt biblioteka baca SignatureException ako potpis ne odgovara server-side secret-u.
Tampered token ili token potpisan proizvoljnim kljucem se odbija pre nego sto se
ista procita iz claims-a.

Dodatno:
- Tokeni sada imaju expiration (8 sati) - sto pre nije bio slucaj
- isTokenValid() proverava i da token nije istekao
- Eksplicitno se hvata SignatureException, ExpiredJwtException, MalformedJwtException

### Izmenjeni fajlovi

- security/JwtService.java

### Pokretanje

./mvnw clean package -DskipTests
java -jar target/web-security.jar

Swagger UI: http://localhost:8080/swagger-ui/index.html
