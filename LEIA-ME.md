# Gastos do Casal (Android)

Registra sozinho **todas as movimentações** (entradas e saídas: crédito, débito, Pix, transferência, boleto)
do **Nubank, PicPay, Neon e Mercado Pago** lendo as notificações dos bancos.
Mostra por mês quanto entrou, quanto saiu e o saldo de cada pessoa e do casal.

## 1. Gerar o aplicativo (uma vez, pelo computador)
1. Entre em github.com e faça login.
2. Clique em **+** (canto de cima) → **New repository** → nome `gastos-do-casal` → **Public** → **Create repository**.
3. Na página que abrir, clique em **uploading an existing file**.
4. Descompacte este .zip no computador e arraste **todo o conteúdo da pasta GastosDoCasal**
   (incluindo a pasta `.github`) para a página. Clique em **Commit changes**.
5. Abra a aba **Actions**: o "Gerar APK" roda por uns 5 minutos até ficar verde ✅.
6. Na página inicial do repositório, em **Releases** (lado direito), vai aparecer o `GastosDoCasal.apk`.

> Se a pasta `.github` não subir, crie o arquivo pelo site: **Add file → Create new file**,
> nome `.github/workflows/gerar-apk.yml`, e cole o conteúdo do arquivo de mesmo nome.

## 2. Instalar no celular (Alex e Mika)
1. No celular, abra `github.com/SEU-USUARIO/gastos-do-casal/releases/latest` e baixe o `GastosDoCasal.apk`
   (ou mande o arquivo pelo WhatsApp).
2. Toque no arquivo → permita "instalar apps desta fonte" → **Instalar**.
3. Abra o app e escolha quem usa aquele celular (Alex ou Mika).
4. Toque em **Ativar** e ligue o "Gastos do Casal" na lista de acesso a notificações.
   - Se aparecer **"Configuração restrita"**: toque em **Está bloqueado?** no app e siga os passos
     (Informações do app → ⋮ → Permitir configurações restritas).

## Não deixar o celular "matar" o app (importante)
Sem isso, o celular pode fechar o app em segundo plano e os gastos param de entrar.

**Samsung A15 (Mika):**
Configurações → Aplicativos → Gastos do Casal → Bateria → **Sem restrições**.
Depois: Configurações → Cuidados com o dispositivo → Bateria → Limites de uso em segundo plano →
confira que o app **não** está em "Apps em suspensão" nem "Apps em suspensão profunda".

**Realme C75x (Alex):**
Configurações → Aplicativos → Gerenciar aplicativos → Gastos do Casal →
**Uso de bateria** → ative "Permitir atividade em segundo plano" e "Permitir inicialização automática".
Na tela de apps recentes, segure o card do app e toque no **cadeado** para travar.

## Como usar
- Entradas (Pix/transferência recebida, estorno, cashback, rendimento) aparecem em **verde**.
- Saídas (compra no crédito/débito, Pix enviado, boleto, saque) aparecem em **vermelho**.
- Botão **+** para registrar dinheiro em espécie (escolha Entrada ou Saída).
- Segure uma movimentação para apagar.
- Menu ⋮ → **Notificações capturadas**: mostra tudo o que o app leu dos bancos e se registrou ou ignorou.
  Se algo não entrar, use "Copiar tudo" e mande para ajustar.

## O que ele ignora de propósito
- **Pagamento de fatura**: a compra no crédito já foi registrada na hora; contar a fatura de novo dobraria o valor.
- Compras negadas/canceladas, Pix agendado, pedidos de cobrança, avisos de limite e códigos de segurança.

## Próximo passo
Sincronizar em tempo real entre os dois celulares (Firebase). Os dados já têm um código único por gasto
preparado para isso.

## Atualizar
Cada vez que um arquivo do repositório muda, o GitHub gera um APK novo. É só instalar por cima: os dados continuam.
