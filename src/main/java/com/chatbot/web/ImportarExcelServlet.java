package com.chatbot.web;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.IOException;

@WebServlet("/admin/importar-excel")
@MultipartConfig
public class ImportarExcelServlet extends HttpServlet {

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException, ServletException {

        Part archivo =
                request.getPart("archivo");

        System.out.println(
                "Archivo recibido: "
                + archivo.getSubmittedFileName()
        );

        response.sendRedirect(
                request.getContextPath()
                + "/DashboardServlet"
        );
    }
}