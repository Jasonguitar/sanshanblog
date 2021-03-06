package xyz.sanshan.auth.security.server.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import xyz.sanshan.auth.security.common.event.AuthRemoteEvent;
import xyz.sanshan.auth.security.server.entity.Client;
import xyz.sanshan.auth.security.server.mapper.ClientMapper;
import xyz.sanshan.auth.security.server.service.AuthClientService;
import xyz.sanshan.auth.security.server.util.client.ClientTokenUtil;
import xyz.sanshan.auth.security.server.vo.ClientInfo;
import xyz.sanshan.common.exception.auth.ClientInvalidException;
import xyz.sanshan.common.util.UUIDUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class AuthClientServiceImpl implements AuthClientService {

    @Autowired
    private ClientMapper clientMapper;

    @Autowired
    private ClientTokenUtil clientTokenUtil;
    @Autowired
    private DiscoveryClient discovery;

    private ApplicationContext context;

    @Autowired
    public AuthClientServiceImpl(ApplicationContext context) {
        this.context = context;
    }


    @Override
    public String apply(String clientId, String secret) throws Exception {
        Client client = getClient(clientId, secret);
        return clientTokenUtil.generateToken(new ClientInfo(client.getCode(), client.getName(), client.getId().toString(), new Date()));
    }

    @Override
    public List<String> getAllowedClient(String serviceId, String secret) {
        Client info = this.getClient(serviceId, secret);
        List<String> clients = clientMapper.selectAllowedClient(info.getId() + "");
        if (clients == null) {
            clients = new ArrayList<String>();
        }
        return clients;
    }


    @Override
    public List<String> getAllowedClient(String serviceId) {
        Client info = getClient(serviceId);
        List<String> clients = clientMapper.selectAllowedClient(info.getId() + "");
        if(clients==null) {
            clients = new ArrayList<String>();
        }
        return clients;
    }


    @Override
    public void validate(String clientId, String secret) throws Exception {
        Client client = new Client();
        client.setCode(clientId);
        client = clientMapper.selectOne(client);
        if (client == null || !client.getSecret().equals(secret)) {
            throw new ClientInvalidException("Client not found or Client secret is  invalid");
        }
    }

    private Client getClient(String clientId) {
        Client client = new Client();
        client.setCode(clientId);
        client = clientMapper.selectOne(client);
        return client;
    }

    private Client getClient(String clientId, String secret) {
        Client client = new Client();
        client.setCode(clientId);
        client = clientMapper.selectOne(client);
        if (client == null || !client.getSecret().equals(secret)) {
            throw new ClientInvalidException("Client not found or Client secret is error!");
        }
        return client;
    }

    @Override
    @Scheduled(cron = "0 0/1 * * * ?")
    public void registryClient() {
        // 自动注册节点
        discovery.getServices().forEach((name) ->{
            Client client = new Client();
            client.setName(name);
            client.setCode(name);
            Client dbClient = clientMapper.selectOne(client);
            if(dbClient==null) {
                client.setSecret(UUIDUtils.generateShortUuid());
                clientMapper.insert(client);
            }else{
                // 主动推送
                final List<String> clients = clientMapper.selectAllowedClient(dbClient.getId() + "");
                final String myUniqueId = context.getId();
                final AuthRemoteEvent event =
                        new AuthRemoteEvent(this, myUniqueId, name, clients);
                context.publishEvent(event);
            }
        });
    }
}
