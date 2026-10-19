package com.example.swd392_se1907_aives.mapper;
import com.example.swd392_se1907_aives.domain.entity.User;
import com.example.swd392_se1907_aives.domain.dto.response.UserResponse;
import org.springframework.stereotype.Component;
import java.util.List;
@Component
public class UserMapper {
  public UserResponse toUserResponse(User user) { return UserResponse.from(user); }
  public List<UserResponse> toUserResponses(List<User> users) { return users.stream().map(UserResponse::from).toList(); }
}
