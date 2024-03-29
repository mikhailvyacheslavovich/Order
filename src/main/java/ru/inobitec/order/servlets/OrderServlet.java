package ru.inobitec.order.servlets;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import jakarta.xml.bind.Marshaller;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.xml.sax.SAXException;
import ru.inobitec.order.dto.MessageDTO;
import ru.inobitec.order.dto.OrderDTO;
import ru.inobitec.order.service.OrderService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;

@WebServlet("/orderServlet")
@RequiredArgsConstructor
@Log4j2
public class OrderServlet extends HttpServlet {
    private static final String SERVLET_CREATE_MESSAGE = "Create successfully";
    private static final String SERVLET_UPDATE_MESSAGE = "Update successfully";
    private static final String SERVLET_DELETE_MESSAGE = "Delete successfully";
    private static final String SERVLET_CREATE = "create";
    private static final String SERVLET_READ = "read";
    private static final String SERVLET_UPDATE = "update";
    private static final String SERVLET_DELETE = "delete";
    private static final String SERVLET_INVALID_COMMAND = "Invalid command ";
    private static final String SERVLET_ERROR_MESSAGE = "Unable to read ";

    private final OrderService orderService;
    private OrderServletHandler orderServletHandler;
    private SAXParser parser;

    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            SAXParserFactory factory = SAXParserFactory.newInstance();
            parser = factory.newSAXParser();
            orderServletHandler = new OrderServletHandler();
        } catch (ParserConfigurationException | SAXException ex) {
            log.error(ex.getMessage());
        }
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response) {
        try {
            parser.parse(request.getInputStream(), orderServletHandler);
            MessageDTO result = orderServletHandler.getMessage();
            response.setContentType("text/html");
            PrintWriter writer = response.getWriter();
            switch (result.getCommand()) {
                case SERVLET_CREATE -> {
                    orderService.addOrder(result.getOrderDTO());
                    writer.println(SERVLET_CREATE_MESSAGE);
                }
                case SERVLET_UPDATE -> {
                    orderService.updateOrder(result.getOrderDTO());
                    writer.println(SERVLET_UPDATE_MESSAGE);
                }
                case SERVLET_DELETE -> {
                    orderService.deleteOrderById(result.getOrderDTO().getId());
                    writer.println(SERVLET_DELETE_MESSAGE);
                }
                case SERVLET_READ -> {
                    OrderDTO order = orderService.getOrderById(result.getOrderDTO().getId());
                    try {
                        JAXBContext context = JAXBContext.newInstance(OrderDTO.class);
                        Marshaller marshaller = context.createMarshaller();
                        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                        StringWriter stringWriter = new StringWriter();
                        marshaller.marshal(order, stringWriter);
                        writer.println(stringWriter);
                    } catch (JAXBException e) {
                        log.error(e.getCause());
                        writer.println(SERVLET_ERROR_MESSAGE);
                        throw new RuntimeException(e);
                    }
                }
                default -> {
                    log.info(SERVLET_INVALID_COMMAND + result.getCommand());
                    writer.println(SERVLET_INVALID_COMMAND);
                }
            }
        } catch (SAXException | IOException ex) {
            log.error(ex.getMessage());
        }
    }
}
