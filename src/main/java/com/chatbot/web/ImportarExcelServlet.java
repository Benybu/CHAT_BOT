package com.chatbot.web;

import com.chatbot.dao.ProductoDAO;
import com.chatbot.model.Producto;
import com.chatbot.dao.IndiceBusquedaBuilder;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;

import java.io.IOException;
import java.math.BigDecimal;

@WebServlet("/admin/importar-excel")
@MultipartConfig
public class ImportarExcelServlet extends HttpServlet {

    private final ProductoDAO productoDAO = new ProductoDAO();

    @Override
    protected void doPost(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws IOException, ServletException {

        Part archivo = request.getPart("archivo");

        Workbook workbook =
                new XSSFWorkbook(archivo.getInputStream());

        Sheet hoja = workbook.getSheetAt(0);

        productoDAO.eliminarTodos();

        boolean primeraFila = true;

        for (Row fila : hoja) {

            // Saltar encabezado
            if (primeraFila) {
                primeraFila = false;
                continue;
            }

            if (fila.getCell(1) == null
                        || getTexto(fila.getCell(1)).isEmpty()) {
                    continue;
            }
            
            Producto p = new Producto();

            p.setSku(getTexto(fila.getCell(0)));

            p.setNombre(getTexto(fila.getCell(1)));

            p.setCategoria(getTexto(fila.getCell(2)));

            p.setMarca(getTexto(fila.getCell(3)));

            p.setModelo(getTexto(fila.getCell(4)));

            p.setMedida(getTexto(fila.getCell(5)));

            p.setAtributos(getTexto(fila.getCell(6)));

            p.setDescripcion(getTexto(fila.getCell(7)));

            p.setTags(getTexto(fila.getCell(8)));

            p.setPrecio(
                    BigDecimal.valueOf(
                            getNumero(fila.getCell(9))
                    )
            );

            p.setStock(
                    (int) getNumero(fila.getCell(10))
            );

            p.setImagen(
                    getTexto(fila.getCell(11))
                            .replace(" ", "")
            );

            p.setActivo(true);

            // Generar índice de búsqueda
            p.setIndiceBusqueda(
                    IndiceBusquedaBuilder.construir(p)
            );

            productoDAO.guardarOActualizar(p);
        }

        workbook.close();

        response.sendRedirect(
                request.getContextPath()
                        + "/DashboardServlet"
        );
    }

    private String getTexto(Cell cell) {

        if (cell == null) {
            return "";
        }

        cell.setCellType(CellType.STRING);

        return cell.getStringCellValue().trim();
    }

    private double getNumero(Cell cell) {

        if (cell == null) {
            return 0;
        }

        if (cell.getCellType() == CellType.NUMERIC) {
            return cell.getNumericCellValue();
        }

        try {
            return Double.parseDouble(
                    cell.toString()
            );
        } catch (Exception e) {
            return 0;
        }
    }
}