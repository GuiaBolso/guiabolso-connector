# Changelog
Todas as mudanças notáveis neste projeto serão documentadas neste arquivo.

O formato é baseado em [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
e este projeto adere ao [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Aguardando publicação]

### Adicionado

#### Redirecionar usuário não autorizado

Possibilidade de redirecionar quando usuário não possui permissão, fazendo com que passe pelo fluxo de
permissão novamente. Configurado pela variável de ambiente `REDIRECT_UNAUTHORIZED_POLICY`, sendo aplicado
de acordo com tipo de eventos, sendo do parceiro ou do usuário. Os valores possíveis são, `USER_EVENTS` para 
eventos do usuário, `PARTNER_EVENTS` para eventos do parceiro, `ALWAYS` para todos e `NEVER` para não utilizar, 
sendo esse valor padrão.

### Alterado
### Corrigido
### Removido

## [1.0.0] - Primeira versão publicada

[Aguardando publicação]: https://github.com/Guiabolso/guiabolso-connector/compare/v1.0.0...HEAD
[1.0.0]: https://github.com/Guiabolso/guiabolso-connector/releases/tag/v1.0.0
