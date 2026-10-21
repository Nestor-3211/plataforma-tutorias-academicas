# Plataforma de Tutorias Academicas Universitarias

Proyecto del curso **Curso Integrador I: Sistemas - Software** (UTP).
Docente: Ruby Donna Villaseca Nunez.

## Integrante y rol

El trabajo es individual.

| Integrante | Rol |
|---|---|
| Nestor Gustavo Choque Martel | Unico integrante: analisis, diseño, desarrollo (backend y vistas), base de datos, pruebas y documentacion |

## De que trata

La idea del proyecto es conectar alumnos que necesitan reforzar un curso
con otros estudiantes que pueden dictarles tutoria. El alumno busca un
tutor por curso, le reserva una sesion, y cuando ya se dicto la paga
desde la misma plataforma. El tutor publica en que curso puede ayudar,
su tarifa y su disponibilidad, y desde su lado va confirmando o
cancelando las reservas que le van llegando. Un administrador ve los
indicadores (KPIs) de la plataforma en un dashboard.

## Que funciona hasta el avance 3

- Registro e inicio de sesion (contraseñas con BCrypt).
- Publicar perfil de tutor y buscar tutores por curso, tarifa y calificacion.
- Reservar sesiones y gestionarlas (confirmar, cancelar, completar).
- Pago de sesiones con calculo de la comision de la plataforma (15%).
- Descargar en Excel el historial de pagos (alumno) y los ingresos (tutor).
- Rol de administrador con un dashboard de KPIs (y exportacion a Excel).

Lo de reseñas y el video tutorial todavia no esta hecho.

## Como esta organizado el codigo

Segui la estructura en capas que vimos en clase:

- **controller**: recibe la peticion HTTP y decide que pagina mostrar.
  Uso `@Controller` de Spring MVC (no hice una API REST), cada metodo
  devuelve el nombre de una plantilla Thymeleaf.
- **service** (y su carpeta `impl`): aca esta la logica del negocio en
  si. Las validaciones, el calculo de la comision, los KPIs, el Excel, y
  cosas como que un tutor no pueda confirmar una reserva que no es suya.
- **dao**: interfaces de Spring Data JPA. La mayoria las dejo vacias
  o con un metodo con nombre largo (tipo `findByCorreo`) y Spring ya
  sabe que hacer solo. Para el filtro de busqueda de tutores, que tiene
  varias condiciones a la vez, tuve que escribir la consulta a mano con
  `@Query`.
- **model**: las clases que representan las tablas (Usuario, Curso,
  PerfilTutor, Reserva, Pago, etc).
- **dto**: objetos simples con los numeros ya calculados del dashboard.
- **config**: `SecurityConfig` (el bean de BCrypt), `DataSeeder` (carga
  roles, universidades, cursos y la cuenta de administrador la primera vez
  que se levanta), `AdminInterceptor` y `WebConfig` (solo el administrador
  entra a las URLs `/admin/**`).
- **exception**: la excepcion de acceso denegado.

## Tecnologias y librerias

- Java 17 o superior (lo probe con Java 18) + Spring Boot 3.3
- Spring Data JPA con Hibernate
- MySQL
- Thymeleaf para las vistas (Bootstrap y Chart.js en el dashboard)
- Spring Security, pero solo para el cifrado BCrypt (todavia no uso los filtros
  completos; el control por rol lo hago con un interceptor propio)
- Lombok
- Maven
- **Guava**: `Preconditions` en las validaciones y `Multiset` para contar en el dashboard
- **Apache Commons Lang3**: `StringUtils` para limpiar y validar datos
- **Logback**: configuracion propia en `logback-spring.xml` (consola + archivo en `logs/`)
- **Apache POI**: generacion de los Excel

## Para levantarlo en local

1. Crear la base de datos vacia en MySQL.
2. Copiar `src/main/resources/application.properties.example` a
   `src/main/resources/application.properties` y poner tu usuario y
   contraseña de MySQL, y los datos del administrador. Ese archivo no se
   sube al repositorio.
3. Correr `TutoriasApplication` desde IntelliJ (o `mvn spring-boot:run`).
4. La primera vez que levanta, el `DataSeeder` carga los datos base
   (roles, universidades, cursos) y crea la cuenta de administrador.
5. Entrar a `http://localhost:8080/registro` y crear una cuenta para
   probar, o a `http://localhost:8080/login` con el administrador para ver
   el dashboard.

## Como probarlo de principio a fin

1. Te registras como **Tutor**, inicias sesion, y en "Publicar perfil"
   eliges un curso, tu tarifa y tu disponibilidad.
2. En otra pestaña (o en incognito) te registras como **Alumno**,
   buscas el curso que publicaste y le reservas una sesion a ese
   tutor.
3. Vuelves a la cuenta del tutor, entras a "Reservas como tutor" y le
   das confirmar. Despues la marcas como completada (como si ya se
   hubiera dictado).
4. Vuelves al alumno, vas a "Mis reservas" y le das pagar a esa
   sesion. Ahi el sistema calcula solo la comision (15%) y cuanto le
   queda al tutor.
5. Revisas "Historial de pagos" (alumno) y "Mis ingresos" (tutor), y
   puedes descargar cada uno en Excel.
6. Entras con el administrador y miras el dashboard: los numeros
   deben reflejar lo que acabas de hacer.

## Reglas que valide

Estas son las validaciones que meti en el Service:

- Una reserva solo se puede pagar si ya esta COMPLETADA, y nada mas
  una vez (si intentas pagarla de nuevo, tira error).
- Un tutor no se puede reservar una sesion a si mismo.
- No puedes reservar un horario que el tutor ya tiene ocupado (a menos
  que esa otra reserva este cancelada).
- Solo el tutor dueño del perfil puede confirmar, cancelar o completar
  una reserva de ese perfil.
- Solo el administrador puede entrar a `/admin/**`.
- La contraseña nunca se guarda tal cual, siempre pasa por BCrypt.

## Flujo de trabajo en Git

- La rama `main` es la rama estable: no trabajo directamente en ella.
- Cada funcionalidad la desarrollo en su propia rama
  (`feature/nombre-de-la-funcionalidad`) y la integro a `main` con un
  Pull Request.
- Los commits son frecuentes y con mensajes que explican que se hizo.

## Documentacion

Los documentos estan en la carpeta `docs/`: descripcion del sistema,
API y funciones, arquitectura, manual de usuario, guias paso a paso, FAQs,
Javadoc y evidencias de las librerias.

## Pendiente

- Modulo de reseñas (la calificacion promedio del dashboard sale vacia hasta entonces).
- Pruebas unitarias (todavia no las tengo).
- `AuthController` y `PerfilTutorController` consultan `UniversidadDAO` y
  `CursoDAO` directamente; lo voy a pasar a un Service.
- La comision del 15% esta fija en el codigo; conviene moverla a la configuracion.
- Video tutorial.
