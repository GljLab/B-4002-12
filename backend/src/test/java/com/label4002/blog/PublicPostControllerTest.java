package com.label4002.blog;

import com.label4002.blog.entity.PostEntity;
import com.label4002.blog.entity.UserEntity;
import com.label4002.blog.repository.PostRepository;
import com.label4002.blog.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PublicPostControllerTest extends BaseIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PostRepository postRepository;

    @BeforeEach
    void setUp() {
        postRepository.deleteAll();
        userRepository.deleteAll();

        UserEntity user = new UserEntity();
        user.setUsername("admin");
        user.setPassword("$2y$10$4U4JmQqeeh8lDjQmrVE4xuQkPfyM4H7stUtDMxAM5BdD6Q7/DP8iq");
        userRepository.save(user);

        PostEntity post = new PostEntity();
        post.setTitle("第一篇");
        post.setContent("这是用于测试的文章内容");
        post.setAuthor(user);
        postRepository.save(post);
    }

    @Test
    void should_list_public_posts() throws Exception {
        mockMvc.perform(get("/api/v1/posts"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].title").value("第一篇"));
    }

    @Test
    void should_return_404_when_post_not_found() throws Exception {
        mockMvc.perform(get("/api/v1/posts/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }
}
