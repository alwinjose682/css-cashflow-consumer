package io.alw.css.cashflowconsumer.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping(path = "cashflow", produces = MediaType.APPLICATION_JSON_VALUE)
public class CashflowReceiverController {
    private record Response(String message, String currentThread) {
    }

    private static Logger log = LoggerFactory.getLogger(CashflowReceiverController.class);

    @GetMapping("/ct")
    public ResponseEntity<Response> ct() {
        String currentThread = Thread.currentThread().toString();
        log.info(currentThread);
        Response response = new Response("The current thread is: ", currentThread);
        return ResponseEntity.ok(response);
//        return ResponseEntity.status(HttpStatus.OK).body(response);

//        var response1 = new HashMap<String, String>();
//        response1.put("The current thread is: ", currentThread);
//        return ResponseEntity.ok(response1);
    }
}
