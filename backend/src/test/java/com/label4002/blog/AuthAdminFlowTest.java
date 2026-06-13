package com.label4002.blog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthAdminFlowTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Long otherPostId;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity admin = new UserEntity();
        admin.setUsername("admin");
        admin.setPassword(passwordEncoder.encode("admin123456"));
        admin = userRepository.save(admin);

        UserEntity other = new UserEntity();
        other.setUsername("other");
        other.setPassword(passwordEncoder.encode("other123456"));
        other = userRepository.save(other);

        PostEntity otherPost = new PostEntity();
        otherPost.setTitle("他人文章");
        otherPost.setContent("不能被admin删除");
        otherPost.setAuthor(other);
        otherPost = postRepository.save(otherPost);
        otherPostId = otherPost.getId();
    }

    @Test
    void should_require_authentication_for_admin_api() throws Exception {
        mockMvc.perform(get("/api/v1/admin/posts/mine"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @Test
    void should_create_update_and_delete_own_post() throws Exception {
        MockHttpSession session = loginAsAdmin();

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/posts")
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"新文章",
                                  "content":"这是一篇新文章"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("新文章"))
                .andReturn();

        JsonNode createNode = objectMapper.readTree(createResult.getResponse().getContentAsString());
        long postId = createNode.get("id").asLong();

        mockMvc.perform(put("/api/v1/admin/posts/{id}", postId)
                        .session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title":"已编辑标题",
                                  "content":"已编辑内容"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("已编辑标题"));

        mockMvc.perform(delete("/api/v1/admin/posts/{id}", postId)
                        .session(session))
                .andExpect(status().isNoContent());
    }

    @Test
    void should_return_403_when_delete_other_user_post() throws Exception {
        MockHttpSession session = loginAsAdmin();

        mockMvc.perform(delete("/api/v1/admin/posts/{id}", otherPostId)
                        .session(session))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    private MockHttpSession loginAsAdmin() throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username":"admin",
                                  "password":"admin123456"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        return (MockHttpSession) loginResult.getRequest().getSession(false);
    }
}
