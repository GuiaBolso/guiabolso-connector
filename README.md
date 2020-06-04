# Guiabolso Connector
O Guiabolso Connector é um cliente desenvolvido em Kotlin, que provê uma camada de abstração com o Guiabolso Connect com o objetivo de tornar _plug-and-play_ a integração com o produto .

Se você estiver interessado apenas em como configurar o Guiabolso Connector para integração no seu ambiente, sugerimos visitar a [área do desenvolvedor](https://guiabolsoconnect.com.br/) no nosso site, você vai encontrar um _quickStart_ com um passo-a-passo de com realizar a integração.

# Pré requisitos
- Java Development Kit 8 ou superior:

# Estrutura do projeto
O projeto usa como arquitetura as práticas de [Clean Architecture](https://8thlight.com/blog/uncle-bob/2012/08/13/the-clean-architecture.html) com o intuito de tornar-se adaptável com as tecnologias/recursos usadas pelos parceiros.

O projeto está dividido nos seguintes módulos:
* `usecases`: Possui toda a regra de negócio do serviço;
* `cache`: Implementação de cache em memória e cache distribuído;
* `events`: Configurações do processador de eventos e seus handlers (análogo aos controllers);
* `aws`: Implementação dos recursos necessários pelo serviço usando a AWS;
* `gcp`: Implementação dos recursos necessários pelo serviço usando a Google Cloud Platform;
* `development`: Implementação dos recursos necessários para ser usado no ambiente de **DESENVOLVIMENTO**;
* `web`: Camada para expor o processador de eventos como um serviço web;
* `application`: Camada que conecta todos os módulos utilizando o framework de injeção de dependência `Spring`.

## Diagrama de dependências entre os módulos
<p align="center">
    <img src="https://github.com/gbprojectbr/guiabolso-connector/raw/master/assets/project_dependency.png" alt="Diagrama">
</p>

# Funcionalidades
* Mapeamento transparente de usuários para seus respectivos tokens de acesso.
* Consolidação de variáveis de vários eventos em um evento único de forma configurável.
* Abstração de toda troca de tokens do protocolo OAuth usado pelo Guiabolso Connect.
* Cache de dados configurável por evento e usuário.

# Tipos de eventos
Os eventos são segmentados em 2 tipos baseado à quem o recurso pertence: 
  - Eventos do próprio parceiro (Todos os eventos relacionados a autenticação)
  - Eventos em nome do usuário (Eventos que disponibiliza informação sobre usuário após autorização)
  
Neste contexto, o Guiabolso Connector tem como papel fazer proxy para o Guiabolso Connect, abstraindo a autenticação de parceiros e usuários de acordo com o _endpoint_ chamado. 

## Eventos do parceiro
Esse tipo de evento está mapeado com o método `POST` na rota `/partner/events/`, eventos chamados nessa rota será autenticado pelo Connector usando as credenciais do parceiro e em seguida encaminhado ao ambiente do [Guiabolso Connect](https://guiabolsoconnect.com.br/).

<p align="center">
    <img src="https://github.com/gbprojectbr/guiabolso-connector/raw/master/assets/project_flow_partner.png" alt="Fluxo de eventos em nome do parceiro" width="300" height="500">
</p>

## Eventos do usuário
Esse tipo de evento está mapeado com método `POST` na rota `/gbConnect/events/`, 
eventos chamados nessa rota serão autenticados tanto pelo usuário quanto pelo parceiro e em seguida encaminhado o ambiente do [Guiabolso Connect](https://guiabolsoconnect.com.br/).

Para que o Connector possa fazer o mapeamento de usuário para credencial é obrigatório enviar o identificador único do usuário no campo `userId` dentro do bloco `identity` do evento.


<p align="center">
    <img src="https://github.com/gbprojectbr/guiabolso-connector/raw/master/assets/project_flow_user.png" alt="Fluxo de eventos em nome do usuário" width="400" height="600">
</p>

## Eventos agregadores de variáveis
O Guiabolso Connect disponibiliza eventos que pode ser usados para consultar variáveis por categoria de um dado usuário,
entretanto esta forma granular de buscar variáveis pode ser modificada criando-se um evento agregador.
Veja fluxo abaixo:
<p align="center">
    <img src="https://github.com/gbprojectbr/guiabolso-connector/raw/master/assets/project_flow_merged.png" alt="Fluxo de eventos agregadores" width="300" height="500">
</p>

Um exemplo, digamos que o Guiabolso Connect possua os seguintes eventos que queremos agrega-los:
* `guiabolso-connector:user:credit:score`: variáveis de score de crédito;
* `guiabolso-connector:user:statistics:income`: variáveis estatísticas sobre a renda do usuário.

Todo evento que devolve variável, devolve uma lista contendo itens com o seguinte formato:
````json
{
  "type": "string",
  "key": "GBCONNECT.USER.CPF",
  "value": "12345678910"
}
````
Onde os tipos podem ser: `int32`, `int64`, `float32`, `float64`, `string` ou `bool`.

Com isso podemos criar um evento agregador que irá mesclar a lista de variáveis dos 2 eventos definindo um agregador:
````yaml
publish:
  type: EVENT
  name: guiabolso-connector:variables
  version: 1
sources:
  - statusKey: GBCONNECT.CREDIT.SCORES.STATUS
    eventName: guiabolso-connector:user:credit:score
    eventVersion: 1
  - statusKey: GBCONNECT.CREDIT.TRANSACTIONS.VARIABLES.STATUS
    eventName: guiabolso-connector:user:statistics:income
    eventVersion: 1
````

Desta forma, um evento chamado `guiabolso-connector:variables` poderá ser chamado no Guiabolso Connector na rota de evento do usuário que ao invés de repassar o evento para o Guiabolso Connect, irá disparar em paralelo os eventos cadastrados no evento agregador.
Quando todos os eventos terminarem os resultados serão mesclados e as variáveis cujas chaves foram definidas em `statusKey` serão adicionadas à lista de variáveis com o status do respectivo evento, status esse que pode ser: `SUCCESS` ou `ERROR: {CAUSA}`, onde as causas podem ser diversas, como:`ACCESS_DENIED`, `AUTHORIZATION_EXPIRED`, `UPGRADE_REQUIRED`, entre outras.

# Cache de eventos
O serviço tem a capacidade de cachear as respostas dos eventos para minimizar custos ou por proteção contra possíveis
instabilidades. Para habilitar o cache em um evento basta adicioná-lo na lista de eventos cacheados:

````yaml
eventCaches:
  - eventName: guiabolso-connector:user:credit:score
    eventVersion: *
    cacheDuration: 7 days
    cacheUsagePolicy: ALWAYS
  - eventName: guiabolso-connector:user:credit:score
    eventVersion: *
    cacheDuration: 30 days
    cacheUsagePolicy: ONLY_ON_FAILURES
  - eventName: guiabolso-connector:user:statistics:income
    eventVersion: *
    cacheDuration: 30 days
    cacheUsagePolicy: ONLY_ON_FAILURES
````

Atualmente é suportado Redis, S3 e Google Storage como ferramentas de cache para os eventos, a implementação possui uma estrutura de nível de cache onde é possível deixar mais de uma estratégia de cache configurada de forma hierárquica.
### Nível de cache 
  - Redis - Nível 1
  - S3 - Nível 2
  - Google Storage - Nível 2
  
Por exemplo, configurar um cache de nível 1 utilizando o Redis mas mantendo os dados somente por no máximo 1 hora e um cache nível 2 utilizando o S3 que respeita o tempo total configurado para o cache.


# Configuração do perfil para executar a aplicação 
O Guiabolso Connector usa os [perfis do Spring](https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-profiles.html) para definir as tecnologias à serem utilizadas, (serviço de cache, serviço de criptografia, etc).

Os possíveis perfis definidos são:

* `development`: Utilizado exclusivamente para execução local com dados mockados;
* `production`: Deve ser utilizado em ambiente de produção;
* `redis`: Deve ser usado para habilitar uso de redis como implementação de cache.
* `aws`: Utilizado para indicar que implementações dos serviços da Amazon devem ser utilizados, depende do profile `production` ativado. . Por exemplo: S3 e ElastiCache para cache, DynamoDB para armazenamento de tokens, etc;
* `gcp`: Utilizado para indicar que implementações dos serviços do Google Cloud Platform devem ser utilizados, depende do profile `production` ativado. Por exemplo: Google Storage, Cloud KMS, Cloud Datastore para armazenamento de tokens, etc.

É possível compor o valor dessa variável com mais de um perfil, se necessário, separando cada perfil por vírgula. Ou seja, digamos que a aplicação deve utilizar as implementações de AWS em produção, essa variável deveria então ser definida como: `SPRING_PROFILES_ACTIVE=production,aws`.
>**_Nota_:** Os perfis `aws`, `gcp` e `development` não podem ser usados ao mesmo tempo.

## Configurações disponíveis por perfil
Cada perfil possui configurações que podem ser alteradas utilizando-se variáveis de ambiente, veja a abaixo a variáveis
disponível em cada perfil:
### Aplicação - Estará disponível para todos os perfis.
 - `CLIENT_ID` - Usado para configurar clientId que deverá ser usado ao fazer integração.
 - `CLIENT_SECRECT` - Senha associada ao `CLIENT_ID`.

### **redis**
- `REDIS_ADDRESS` - Url de conexão com redis, ex: `redis://master-localhost:6379,redis://slave-localhost:6379`.
- `REDIS_EXPIRE_DURATION_MINUTES` - Configura em quanto tempo após o cache ser gravado no Redis ele deve expirado.

### **aws - Amazon Web Services**
- `DYNAMODB_REGION` - Região onde a tabela no banco Dynamo foi configurada.
- `DYNAMODB_TABLE` - Nome da table no banco Dynamo que deverá ser usada.
- `KMS_ENCRYPTION_KEY` - Chanve de criptografia a ser usada.
- `KMS_SERVICE_ENDPOINT` - Url de conexão com KMS.
- `KMS_SIGNING_REGION` - Região onde a chave KMS foi criada.
- `KMS_CACHE_CAPACITY` - Tamanho máximo de cache de chave a ser usada.
- `KMS_CACHE_MAXAGEMINUTES` - Determina o máximo de tempo até a chave KMS ser renovada.
- `KMS_CACHE_MESSAGEUSELIMIT` - Define a quantidade de vezes em que uma mesma chave pode ser usada para criptografar dados.
- `S3_BUCKET_NAME` - Nome do bucket S3.
- `S3_SERVICE_ENDPOINT` - Url de conexão com S3.
- `S3_SIGNING_REGION` - Região onde o buckert S3 foi criado.
- `S3_EXPIRE_DURATION_MINUTES` - Configura em quanto tempo após o cache ser gravado no S3 ele deve ser considerado expirado.

### **gcp - Google Cloud Platform**
- `CLOUD_KMS_PROJECT` - Nome do projeto Google Cloud à qual o KMS esta associado.
- `CLOUD_KMS_LOCATION` - Região onde foi criado o KMS.
- `CLOUD_KMS_CRYPTOKEY` - Nome da chave KMS a ser usada.
- `CLOUD_KMS_KEYRING` - Nome keyring associada à chave KMS.
- `STORAGE_BUCKET_NAME` - Nome do bucket Cloud Storage
- `STORAGE_EXPIRE_DURATION_MINUTES` - Configura em quanto tempo após o cache ser gravado no Cloud Storage ele deve ser considerado expirado.

## Criptografia
O serviço de criptografia usado depende de qual perfil foi configurado no momento da inicialização do serviço, será utilizado o serviço de criptografia para:
  - Descriptografar as credenciais (clientId e clientSecret) do parceiro, que devem ser enviadas como parâmetro na inicialização do serviço de forma criptografada.
  - Criptografar e descriptografar os tokens de acesso de usuários.
  - Criptografar e descriptografar caches das respostas dos eventos.


## Banco de dados
Implementações de banco de dados são utilizadas no projeto para o armazenamento dos tokens dos usuários.

Atualmente há as seguintes implementações de banco de dados já incluídas no projeto:
* Não-relacional usando DynamoDB;
* Não-relacional usando Google Cloud Datastore
* Banco de dados em memória local não recomendado para uso em produção, apenas testes em desenvolvimento;

## Métricas e APM
Usa a biblioteca de [Tracing](https://github.com/GuiaBolso/events-protocol) que atualmente suporta Datadog e New Relic.

# Executando Guiabolso Connector em desenvolvimento
As configurações abaixo são recomendadas apenas para testes em desenvolvimento, para configurações para ambiente de produção, é altamente recomendavél configuração de um seriço de criptografia conforme mencionando nas seções anteriores.

Para executar a aplicação, execute o seguinte comando na linha de comando:
```bash
  CLIENT_ID=encrypted.SEU_CLIENT_ID CLIENT_SECRET=encrypted.SEU_CLIENT_SECRED ./gradlew run
```
