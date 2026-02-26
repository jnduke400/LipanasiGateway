package com.hybrid9.pg.Lipanasi.convertor;

import lombok.AllArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Component;

@Component
@AllArgsConstructor
public class DTOConvertor<T,E> {

    private ModelMapper modelMapper;

    public E convertToEntity(T dto, Class<E> entityClass) {
        E entity = modelMapper.map(dto, entityClass);
        return entity;
    }

    public T convertToDto(E entity, Class<T> dtoClass) {
        T dto = modelMapper.map(entity, dtoClass);
        return dto;
    }

    public E convertQnToEntity(T dto, Class<E> entityClass) {
        E entity = modelMapper.map(dto, entityClass);
        return entity;
    }


}
