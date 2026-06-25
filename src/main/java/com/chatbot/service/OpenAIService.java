package com.chatbot.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class OpenAIService {

    private static final String API_KEY =
            System.getenv("GEMINI_API_KEY");

    private static final String URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key="
                + API_KEY;

    private final OkHttpClient client =
            new OkHttpClient();

    public String preguntar(String prompt) {

        try {

            JSONObject json = new JSONObject();

            json.put(
                    "model",
                    "llama-3.1-8b-instant"
            );

            JSONArray messages =
                    new JSONArray();

            JSONObject system =
                    new JSONObject();

            system.put("role", "system");

            system.put(
                "content",
                """
                Eres un vendedor profesional de tecnología y productos gamer para Marketplace.

                TU ÚNICA FUENTE DE INFORMACIÓN
                son EXCLUSIVAMENTE los productos enviados dentro del prompt.

                REGLAS OBLIGATORIAS:

                1. SOLO puedes responder utilizando:
                - productos existentes
                - precios reales
                - stock real
                - marcas reales
                - categorías reales
                - información incluida en la base de datos enviada.

                2. ESTÁ TOTALMENTE PROHIBIDO:
                - inventar productos
                - inventar modelos
                - inventar marcas
                - inventar precios
                - inventar stock
                - inventar características técnicas
                - inventar categorías
                - inventar promociones
                - inventar accesorios
                - asumir información no proporcionada.

                3. Si el usuario pregunta por un producto
                que NO existe en el inventario:

                RESPONDE de manera amable indicando que:
                - actualmente no está disponible
                - no se encontró en inventario
                - puedes recomendar alternativas REALES disponibles.

                4. SOLO puedes recomendar:
                - productos exactos existentes
                - o productos similares REALES
                presentes en la base de datos.

                5. JAMÁS menciones productos
                que no aparezcan en el inventario enviado.

                6. Nunca inventes compatibilidades,
                especificaciones o rendimiento técnico
                si no aparecen explícitamente en los datos.

                7. Debes responder como un vendedor humano real:
                - amigable
                - natural
                - breve
                - claro
                - convincente
                - estilo Marketplace
                - estilo tienda gamer moderna.

                8. Usa emojis moderadamente
                para hacer la conversación más natural.

                9. Prioriza respuestas cortas y útiles.

                10. Si el usuario pregunta precio,
                responde mostrando:
                - nombre del producto
                - precio
                - stock si existe.

                11. Si el usuario pide recomendaciones,
                recomienda SOLO productos reales del inventario.

                12. Si existen múltiples productos similares,
                muestra máximo 3 opciones relevantes.

                13. Nunca respondas como una IA general.
                NO eres ChatGPT.
                NO eres un asistente universal.

                SOLO eres el vendedor oficial de esta tienda.

                14. Siempre responde en español.

                15. Si el usuario saluda o conversa,
                responde cordialmente como un vendedor real.

                16. Si el usuario pide algo fuera del catálogo,
                redirige la conversación hacia productos disponibles.

                17. No uses respuestas excesivamente largas.

                18. Tu objetivo principal es:
                - ayudar al cliente
                - recomendar productos reales
                - responder usando únicamente el inventario disponible.
                """
                );

            JSONObject user =
                    new JSONObject();

            user.put("role", "user");

            user.put("content", prompt);

            messages.put(system);
            messages.put(user);

            json.put("messages", messages);

            RequestBody body =
                    RequestBody.create(
                            json.toString(),
                            MediaType.parse(
                                    "application/json"
                            )
                    );

            Request request =
                    new Request.Builder()
                            .url(URL)
                            .addHeader(
                                    "Authorization",
                                    "Bearer " + API_KEY
                            )
                            .addHeader(
                                    "Content-Type",
                                    "application/json"
                            )
                            .post(body)
                            .build();

            Response response =
                    client.newCall(request)
                            .execute();

            String responseBody =
                    response.body().string();

            if (!response.isSuccessful()) {

                System.out.println(responseBody);

                return "Error GROQ: " + responseBody;
            }

            JSONObject obj =
                    new JSONObject(responseBody);

            return obj
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (IOException e) {

            e.printStackTrace();

            return "Error conectando con GROQ";
        }
    }
}