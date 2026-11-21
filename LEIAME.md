# Deus — Forge 1.20.1

## Gerar o .jar (sem instalar nada)
1. Crie um repositório no GitHub e envie o conteúdo desta pasta (incluindo `.github`).
2. Aba **Actions** -> "Compilar Deus" -> **Run workflow**.
3. Baixe o artefato `deus-jar`. Coloque o .jar em `mods/` (Forge 1.20.1, no cliente E no servidor).

## Configurar (obrigatório)
`config/deus-common.toml`: troque `SEU_NICK_AQUI` pelo seu nick/UUID.

## Virar Deus (sem item)
- Digite `/deus`. Se você está na lista do config e ainda não há outro Deus, você se torna O Deus.
  Fica salvo no mundo: morrer ou reiniciar o servidor não tira.
- Qualquer outra pessoa que digitar `/deus` vai para o Inferno na hora (só existe um Deus).
- Nada na mão. **Aperte a rodinha do mouse (botão do meio)** para abrir a roda de poderes.
- Mundos antigos: o item "Deus" deixou de existir. Digite `/deus` uma vez.

## Trono
- `/give SeuNick deus:trono`. Botão direito senta; Shift levanta. A pose é de rei: costas eretas, pernas abertas,
  pés no chão, mãos nos braços do trono. A animação de sentar/levantar mistura a pose aos poucos.
- Ajustes: `PES_SENTADO` em `AssentoEntity.java` (altura); ângulos em `TronoModel.java` (pernas, braços).

## Realidade (mira onde você olha)
- Botões **Raio** (16/32/64/96) e **Modo**: **Superfície** (só transforma o chão) ou **Domínio** (depois da onda,
  uma cúpula de bedrock sobe do chão e fecha no topo, trancando todos dentro). Restaurar remove a cúpula.
  O bloco da cúpula é configurável (`cupula_bloco`).
- **Vazio**, **Abismo** (Warden), **Nether** (vários biomas do Nether), **Corrupção** (deltas de basalto: basalto,
  blackstone, magma, cinzas e o som do bioma), **Limpar** (remove tudo acima do chão: árvores, plantas,
  construções; fica o terreno), **Restaurar** (desfaz tudo, da mais nova para a mais antiga).
- **Biomas…**: Deserto, Floresta, Taiga, Neve, Selva, Savana, Mesa, Sakura (cerejeiras), Pântano, Cogumelos,
  Picos de gelo, Floresta sombria, Planície. Mudam bioma, chão, plantas e plantam árvores.
- Efeitos: sons do Hand of God (carga, onda, desfeita, pulso, onda de choque) e raios visuais sem dano.

## Onisciência
- **Painel de jogadores**: Detalhes (inventário, armadura, vida...; clique no item para retirar; remover
  equipamentos; entregar o item da mão; salvar/restaurar inventário), Ir, Trazer, Observar.
- **Observar / Vagar**: seu espírito sai (modo espectador) e **seu corpo fica onde estava**, com a sua aparência
  (sentado no trono se você estava nele). Observando, Shift solta a câmera e você passa a voar livremente.
  `,` e `.` alternam entre até 4 jogadores marcados. **Backspace** (ou o botão do painel) volta ao corpo.
  Se atacarem o corpo ou alguém entrar no radar, você volta sozinho.
- **Vagar** (poder próprio): sai voando sem escolher ninguém.
- Radar (100 blocos jogadores / 50 hostis), Visão mineral e de entidades (através das paredes),
  Escuta de sussurros (desligada no config; ligada, todos recebem o aviso ao entrar).

## Tempo
Chuva, tempestade, céu limpo, dia, noite, horário personalizado.

## Em breve
Surpreender (teleporte nas costas + paralisia de 5 s + tecla X), O Infinito, clones, Telecinese, Poderes Dark,
Divindade, mundos (Limbo, Céu, Inferno) e o Olimpo.
