UPDATE user_notifications
SET message = CASE title
        WHEN 'You bought an artwork' THEN regexp_replace(message,
            '^You bought «(.*)» for (.*)\. Pay your order so the artist can ship it\.$',
            'Compraste «\1» por \2. Paga tu pedido para que el artista pueda enviarlo.')
        WHEN 'You won the auction!' THEN regexp_replace(message,
            '^You won «(.*)» for (.*)\. Pay your order so the artist can ship it\.$',
            'Ganaste «\1» por \2. Paga tu pedido para que el artista pueda enviarlo.')
        WHEN 'Your artwork sold' THEN regexp_replace(message,
            '^«(.*)» sold for (.*)\. We will notify you when the buyer pays\.$',
            '«\1» se vendió por \2. Te avisaremos cuando el comprador pague.')
        WHEN 'Your auction ended without bids' THEN regexp_replace(message,
            '^«(.*)» received no bids\. You can create another auction\.$',
            '«\1» no recibió pujas. Puedes crear otra subasta.')
        WHEN 'Payment declined' THEN 'Revisa tu método de pago e inténtalo de nuevo.'
        WHEN 'Payment approved' THEN 'El artista preparará el envío.'
        WHEN 'The buyer paid' THEN 'Ya puedes enviar la obra.'
        WHEN 'You have been outbid' THEN regexp_replace(message,
            '^Someone bid (.*) for «(.*)»\. Bid again before the auction ends\.$',
            'Alguien pujó \1 por «\2». Vuelve a pujar antes de que termine la subasta.')
        WHEN 'Your artwork is on its way' THEN regexp_replace(message,
            '^«(.*)» was shipped with (.*)\. Tracking number: (.*)\. Confirm delivery when it arrives\.$',
            '«\1» fue enviada por \2. Número de guía: \3. Confirma la entrega cuando llegue.')
        WHEN 'Delivery confirmed' THEN regexp_replace(message,
            '^The buyer received «(.*)»\.$', 'El comprador recibió «\1».')
        WHEN 'You received a review' THEN regexp_replace(regexp_replace(message,
            '^A buyer rated', 'Un comprador rated'),
            '^(.*) rated «(.*)» with ([1-5]) (estrella|stars)\.$',
            '\1 calificó «\2» con \3 de 5 estrellas.')
    END,
    title = CASE title
        WHEN 'You bought an artwork' THEN 'Compraste una obra'
        WHEN 'You won the auction!' THEN '¡Ganaste la subasta!'
        WHEN 'Your artwork sold' THEN 'Vendiste tu obra'
        WHEN 'Your auction ended without bids' THEN 'Tu subasta terminó sin pujas'
        WHEN 'Payment declined' THEN 'Pago rechazado'
        WHEN 'Payment approved' THEN 'Pago aprobado'
        WHEN 'The buyer paid' THEN 'El comprador ya pagó'
        WHEN 'You have been outbid' THEN 'Superaron tu puja'
        WHEN 'Your artwork is on its way' THEN 'Tu obra está en camino'
        WHEN 'Delivery confirmed' THEN 'Entrega confirmada'
        WHEN 'You received a review' THEN 'Recibiste una reseña'
    END
WHERE title IN ('You bought an artwork', 'You won the auction!', 'Your artwork sold',
    'Your auction ended without bids', 'Payment declined', 'Payment approved', 'The buyer paid',
    'You have been outbid', 'Your artwork is on its way', 'Delivery confirmed', 'You received a review');
