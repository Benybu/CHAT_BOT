package com.chatbot.service;

import okhttp3.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

public class OpenAIService {

    private static final String API_KEY =
            System.getenv("GROQ_API_KEY");

    private static final String URL =
            "https://api.groq.com/openai/v1/chat/completions";

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
                Eres un asistente comercial inteligente para Marketplace.

                TU ÚNICA FUENTE DE VERDAD
                son los productos enviados en el prompt.

                REGLAS OBLIGATORIAS:

                1. SOLO puedes recomendar productos
                que existan en la lista proporcionada.

                2. ESTÁ PROHIBIDO:
                - inventar productos
                - inventar marcas
                - inventar precios
                - inventar stock
                - inventar categorías

                3. Si el usuario pide un producto
                que NO existe en la base de datos:

                - NO inventes alternativas falsas
                - NO menciones productos inexistentes
                - responde que actualmente no está disponible

                4. SOLO puedes recomendar:
                - productos exactos existentes
                - o productos similares REALES
                de la lista proporcionada.

                5. Responde como un vendedor humano:
                - amigable
                - breve
                - natural
                - profesional
                - estilo Marketplace

                6. Usa emojis moderadamente.

                7. Nunca hables de productos
                fuera del inventario.

                8. Si no existe coincidencia,
                invita al usuario a ver
                otras opciones disponibles.

                9. NO actúes como ChatGPT general.
                SOLO eres un vendedor de esta tienda.

                10. Responde SIEMPRE en español.
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