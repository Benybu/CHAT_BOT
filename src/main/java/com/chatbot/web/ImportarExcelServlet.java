package com.chatbot.web;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.*;

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

        Workbook workbook =
                new XSSFWorkbook(
                        archivo.getInputStream()
                );

        Sheet hoja =
                workbook.getSheetAt(0);

        for (Row fila : hoja) {

            Cell celda =
                    fila.getCell(0);

            if (celda != null) {

                System.out.println(
                        "Fila: "
                                + fila.getRowNum()
                                + " -> "
                                + celda.toString()
                );
            }
        }

        workbook.close();

        response.sendRedirect(
                request.getContextPath()
                        + "/DashboardServlet"
        );
    }
}