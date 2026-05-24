package com.example.hostgame.api;

import com.example.hostgame.service.RoomJoinException;
import com.example.hostgame.service.RoomNotFoundException;
import com.example.hostgame.service.ActionRejectedException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(RoomNotFoundException.class)
    public ProblemDetail handleRoomNotFound(RoomNotFoundException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, exception.getMessage());
        detail.setTitle("Room not found");
        detail.setProperty("createdAt", Instant.now());
        return detail;
    }

    @ExceptionHandler(RoomJoinException.class)
    public ProblemDetail handleRoomJoin(RoomJoinException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        detail.setTitle("Room cannot be joined");
        detail.setProperty("createdAt", Instant.now());
        return detail;
    }

    @ExceptionHandler(ActionRejectedException.class)
    public ProblemDetail handleActionRejected(ActionRejectedException exception) {
        ProblemDetail detail = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        detail.setTitle("Action rejected");
        detail.setProperty("createdAt", Instant.now());
        return detail;
    }
}
