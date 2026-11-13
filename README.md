# Gash's Quick Commands

Mod **100% client-side**, por **Gasharaujo**, para Forge 1.20.1 e NeoForge 1.21.1 / 26.1.2. Não precisa ser instalado no servidor e não registra pacotes próprios.

## Uso

- Digite `/gqc` ou `/gashQuickCommands` para abrir a interface. O atalho para abrir começa **sem tecla atribuída**.
- Crie uma lista, informe um nome único e escreva **um envio por linha**. Linhas vazias são ignoradas. Use `/` para comandos; linhas sem `/` são mensagens normais do chat.
- Clique em **Salvar** para gravar. Salvar não executa a lista. O nome inicial é `list 1`, `list 2`, etc.; nomes não se repetem, inclusive ignorando maiúsculas/minúsculas.
- O botão **Exemplos**, no canto superior direito, substitui os comandos da lista em edição pelo exemplo escolhido. O nome, o delay e o atalho são mantidos. Inclui `hello world`, `Safe Gamerules`, `FTBEssentials Unlimited Homes` e `FTBEssentials no cd/warmup`, com os comandos fornecidos. Salve a lista para manter as alterações. Os comandos de FTB dependem dos mods/permissões correspondentes no servidor.
- **Executar** envia a lista salva em ordem. **Executar todas** percorre as listas na ordem exibida. Apenas uma sequência pode executar por vez; **Parar sequência** cancela os envios pendentes. Desconectar também cancela a execução.
- O botão **▶** ao lado de cada lista executa rapidamente aquela lista salva com seu delay configurado, sem precisar selecioná-la. Alterações em edição são preservadas.
- O delay é em milissegundos. Cada lista pode usar um valor próprio ou `-1` para herdar o padrão da aba **Config / Conflitos**. Padrão inicial: 1000 ms. Mesmo com delay zero, é enviado no máximo um comando por tick. A precisão depende dos ticks do cliente.
- Clique no atalho da lista e pressione uma tecla ou botão do mouse; Ctrl, Alt ou Shift + tecla também são aceitos. Esc, Delete ou Backspace remove o atalho. Salve para aplicar à lista.
- Os atalhos das listas aparecem também em **Opções → Controles → Teclas**. A GUI mostra com quais ações de outros mods ou do Minecraft há conflito; passe o mouse sobre o aviso para ver o texto completo. A aba de configuração mostra o relatório completo com paginação. Conflitos são avisos e não impedem salvar.

## Configured (opcional)

O mod funciona sem Configured. Com ele instalado, a configuração CLIENT nativa fornece `defaultDelayMs` e `keybindConflicts` ao menu gerado pelo Configured. `keybindConflicts` é um diagnóstico com os nomes das ações conflitantes; edições manuais desse relatório são substituídas pelo cliente. Não é necessário Configured para detectar conflitos. A tela própria permanece acessível pelo botão de configuração do mod e pelos comandos.

## Arquivos

- Listas: `config/gash_quick_commands/lists.json` dentro da instância.
- Configuração nativa: `config/gash_quick_commands-client.toml`.
- Atalho para abrir: opções de controles do Minecraft (`options.txt`).

As listas são locais à instância e persistem entre mundos/servidores. Escritas usam arquivo temporário e substituição atômica quando suportada. Um JSON inválido é preservado e copiado para um backup `.invalid-<timestamp>`; o erro é registrado no log. Cada linha aceita até 256 caracteres, seguindo o limite do chat. Comandos são enviados como o jogador: permissões do servidor e regras contra spam continuam valendo.

## Desenvolvimento

| Projeto | Minecraft | Loader de compilação | Java |
| --- | --- | --- | --- |
| `forge-1.20.1` | 1.20.1 | Forge 47.4.10 | 17 |
| `neoforge-1.21.1` | 1.21.1 | NeoForge 21.1.248 | 21 |
| `neoforge-26.1.2` | 26.1.2 | NeoForge 26.1.2.109 | 25 |

Cada projeto tem seu próprio Gradle Wrapper. Execute `gradlew.bat build` no projeto desejado, com o JDK apropriado disponível. O código compartilhado fica em `common`; as telas 1.20.1/1.21.1 ficam em `legacy-ui`. O módulo 26.1.2 adapta entrada e renderização para a nova API.

`build` executa também `coreTest`, cobrindo persistência, nomes, validação, ordem, delays, cancelamento e falha de envio. `runClient` abre um cliente de desenvolvimento. Metadados de dependências usam somente limites mínimos (`[versão,)`), sem versão máxima. Isso não garante compatibilidade binária com qualquer versão futura; instale o JAR correspondente ao Minecraft e loader usados.

No Windows, `powershell -ExecutionPolicy Bypass -File tools/build.ps1 -Install` compila, reúne os JARs em `artifacts` e copia apenas este mod para as instâncias de teste `20.1`, `21` e `26.1.2`. Use `-Offline` se as dependências já estiverem no cache.

## Fontes das APIs

- [NeoForge: key mappings](https://docs.neoforged.net/docs/1.21.1/misc/keymappings/)
- [Forge: key mappings](https://docs.minecraftforge.net/en/1.20.1/misc/keymappings/)
- [Configured: sistemas de configuração suportados](https://github.com/MrCrayfish/Configured)

Copyright © 2026 Gasharaujo. Todos os direitos reservados.
