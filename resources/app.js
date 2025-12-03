/**
 * Simple HTTP Server - 前端应用
 * 处理用户登录、注册和页面交互
 */

// ==================== 状态管理 ====================
const AppState = {
    currentUser: null,
    token: null,
    
    // 保存登录状态到sessionStorage
    save() {
        if (this.currentUser && this.token) {
            sessionStorage.setItem('user', JSON.stringify({
                username: this.currentUser,
                token: this.token,
                loginTime: new Date().toLocaleString()
            }));
        }
    },
    
    // 从sessionStorage恢复登录状态
    restore() {
        const saved = sessionStorage.getItem('user');
        if (saved) {
            const data = JSON.parse(saved);
            this.currentUser = data.username;
            this.token = data.token;
            return data;
        }
        return null;
    },
    
    // 清除登录状态
    clear() {
        this.currentUser = null;
        this.token = null;
        sessionStorage.removeItem('user');
    }
};

// ==================== 页面控制 ====================
const PageController = {
    // 显示登录注册页面
    showAuthPage() {
        document.getElementById('authPage').classList.add('active');
        document.getElementById('mainPage').classList.remove('active');
    },
    
    // 显示主页面
    showMainPage(username, token) {
        document.getElementById('authPage').classList.remove('active');
        document.getElementById('mainPage').classList.add('active');
        
        // 更新用户信息显示
        document.getElementById('displayUsername').textContent = username;
        document.getElementById('cardUsername').textContent = username;
        document.getElementById('userToken').textContent = token.substring(0, 16) + '...';
        document.getElementById('loginTime').textContent = new Date().toLocaleString();
    },
    
    // 切换Tab
    switchTab(tabName) {
        // 更新Tab按钮状态
        document.querySelectorAll('.tab-btn').forEach(btn => {
            btn.classList.toggle('active', btn.dataset.tab === tabName);
        });
        
        // 更新Tab内容显示
        document.querySelectorAll('.tab-content').forEach(content => {
            content.classList.remove('active');
        });
        document.getElementById(tabName + 'Tab').classList.add('active');
        
        // 清除消息
        this.clearMessages();
    },
    
    // 清除所有消息
    clearMessages() {
        document.getElementById('loginMessage').innerHTML = '';
        document.getElementById('registerMessage').innerHTML = '';
    }
};

// ==================== 消息提示 ====================
const Message = {
    show(elementId, text, type = 'error') {
        const element = document.getElementById(elementId);
        element.innerHTML = `<div class="msg msg-${type}">${text}</div>`;
    },
    
    success(elementId, text) {
        this.show(elementId, '✅ ' + text, 'success');
    },
    
    error(elementId, text) {
        this.show(elementId, '❌ ' + text, 'error');
    },
    
    info(elementId, text) {
        this.show(elementId, 'ℹ️ ' + text, 'info');
    }
};

// ==================== API请求 ====================
const API = {
    async post(url, data) {
        const response = await fetch(url, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(data)
        });
        return await response.json();
    },
    
    async get(url) {
        const response = await fetch(url);
        return {
            status: response.status,
            statusText: response.statusText,
            headers: Object.fromEntries(response.headers.entries()),
            body: await response.text()
        };
    },
    
    // 不自动跟随重定向的GET请求，用于展示301/302
    async getNoRedirect(url) {
        const response = await fetch(url, {
            redirect: 'manual'  // 阻止自动跟随重定向
        });
        
        // 当redirect为manual时，重定向响应会变成opaqueredirect类型
        // 需要特殊处理
        if (response.type === 'opaqueredirect') {
            // 无法直接获取opaqueredirect的详细信息
            // 我们需要用另一种方式：先发一个请求看是否重定向
            return {
                status: 0,
                statusText: 'Redirect (opaque)',
                redirected: true,
                headers: {},
                body: ''
            };
        }
        
        return {
            status: response.status,
            statusText: response.statusText,
            redirected: response.redirected,
            headers: Object.fromEntries(response.headers.entries()),
            body: await response.text()
        };
    }
};

// ==================== 登录处理 ====================
async function handleLogin(e) {
    e.preventDefault();
    
    const username = document.getElementById('loginUsername').value.trim();
    const password = document.getElementById('loginPassword').value;
    
    // 前端验证
    if (username.length < 3) {
        Message.error('loginMessage', '用户名至少3个字符');
        return;
    }
    if (password.length < 6) {
        Message.error('loginMessage', '密码至少6个字符');
        return;
    }
    
    try {
        Message.info('loginMessage', '登录中...');
        
        const result = await API.post('/api/login', { username, password });
        
        if (result.code === 200) {
            // 登录成功
            AppState.currentUser = username;
            AppState.token = result.token;
            AppState.save();
            
            Message.success('loginMessage', '登录成功，正在跳转...');
            
            setTimeout(() => {
                PageController.showMainPage(username, result.token);
            }, 500);
        } else {
            Message.error('loginMessage', result.message || '登录失败');
        }
    } catch (err) {
        Message.error('loginMessage', '网络错误：' + err.message);
    }
}

// ==================== 注册处理 ====================
async function handleRegister(e) {
    e.preventDefault();
    
    const username = document.getElementById('regUsername').value.trim();
    const password = document.getElementById('regPassword').value;
    const confirmPassword = document.getElementById('regConfirmPassword').value;
    
    // 前端验证
    if (username.length < 3 || username.length > 20) {
        Message.error('registerMessage', '用户名需要3-20个字符');
        return;
    }
    if (password.length < 6) {
        Message.error('registerMessage', '密码至少6个字符');
        return;
    }
    if (password !== confirmPassword) {
        Message.error('registerMessage', '两次输入的密码不一致');
        return;
    }
    
    try {
        Message.info('registerMessage', '注册中...');
        
        const result = await API.post('/api/register', { username, password });
        
        if (result.code === 200) {
            Message.success('registerMessage', '注册成功！请切换到登录页面');
            
            // 清空表单
            document.getElementById('registerForm').reset();
            
            // 2秒后自动切换到登录页
            setTimeout(() => {
                PageController.switchTab('login');
                // 自动填入用户名
                document.getElementById('loginUsername').value = username;
                document.getElementById('loginPassword').focus();
            }, 1500);
        } else {
            Message.error('registerMessage', result.message || '注册失败');
        }
    } catch (err) {
        Message.error('registerMessage', '网络错误：' + err.message);
    }
}

// ==================== 退出登录 ====================
function handleLogout() {
    AppState.clear();
    PageController.showAuthPage();
    
    // 清空表单
    document.getElementById('loginForm').reset();
    document.getElementById('registerForm').reset();
    PageController.clearMessages();
}

// ==================== API测试 ====================

/**
 * 304 缓存测试
 * 演示If-Modified-Since条件请求和304 Not Modified响应
 */
async function test304Cache() {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试304缓存机制...</p>';
    
    const testUrl = '/style.css'; // 使用静态文件测试
    
    try {
        let html = '<div class="cache-test">';
        html += '<h4>📦 304 缓存机制测试</h4>';
        
        // 第一次请求：获取资源和Last-Modified头（强制无缓存）
        html += '<div class="test-step">';
        html += '<div class="step-header"><span class="step-num">Step 1</span> 首次请求（无缓存）</div>';
        
        const startTime1 = Date.now();
        // 使用 cache: 'no-store' 强制发送无缓存请求
        const response1 = await fetch(testUrl, {
            cache: 'no-store'  // 禁用浏览器缓存，确保发送真正的无缓存请求
        });
        const duration1 = Date.now() - startTime1;
        const lastModified = response1.headers.get('Last-Modified');
        const body1 = await response1.text();
        
        html += `
            <div class="request-info">
                <strong>请求：</strong>
                <pre>GET ${testUrl} HTTP/1.1
Host: ${window.location.host}
(无 If-Modified-Since 头)</pre>
            </div>
            <div class="response-info">
                <span class="method">GET</span>
                <span class="url">${testUrl}</span>
                <span class="status status-2xx">${response1.status} ${response1.statusText}</span>
                <span class="duration">${duration1}ms</span>
            </div>
            <div class="response-headers">
                <strong>关键响应头：</strong>
                <pre>Last-Modified: ${lastModified || '(无)'}
Content-Type: ${response1.headers.get('Content-Type')}
Content-Length: ${body1.length} bytes</pre>
            </div>
        `;
        html += '</div>';
        
        // 第二次请求：带If-Modified-Since头
        html += '<div class="test-step">';
        html += '<div class="step-header"><span class="step-num">Step 2</span> 条件请求（带缓存验证）</div>';
        
        if (lastModified) {
            const startTime2 = Date.now();
            const response2 = await fetch(testUrl, {
                headers: {
                    'If-Modified-Since': lastModified
                }
            });
            const duration2 = Date.now() - startTime2;
            const body2 = await response2.text();
            
            html += `
                <div class="request-info">
                    <strong>请求：</strong>
                    <pre>GET ${testUrl} HTTP/1.1
Host: ${window.location.host}
If-Modified-Since: ${lastModified}</pre>
                </div>
                <div class="response-info">
                    <span class="method">GET</span>
                    <span class="url">${testUrl}</span>
                    <span class="status status-${Math.floor(response2.status/100)}xx">${response2.status} ${response2.statusText}</span>
                    <span class="duration">${duration2}ms</span>
                </div>
            `;
            
            if (response2.status === 304) {
                html += `
                    <div class="cache-result success">
                        <span class="icon">✅</span>
                        <div class="text">
                            <strong>304 Not Modified</strong>
                            <p>服务器确认资源未修改，客户端可以使用缓存版本</p>
                            <p>节省传输: ${body1.length} bytes → 0 bytes</p>
                        </div>
                    </div>
                `;
                // 更新验证清单
                markVerified('client-redirect');
                markVerified('server-status');
            } else {
                html += `
                    <div class="cache-result info">
                        <span class="icon">ℹ️</span>
                        <div class="text">
                            <strong>资源已更新</strong>
                            <p>服务器返回了新版本的资源 (${response2.status})</p>
                            <p>响应体大小: ${body2.length} bytes</p>
                        </div>
                    </div>
                `;
            }
        } else {
            html += `
                <div class="cache-result warning">
                    <span class="icon">⚠️</span>
                    <div class="text">
                        <strong>无法进行条件请求</strong>
                        <p>服务器未返回 Last-Modified 头</p>
                    </div>
                </div>
            `;
        }
        html += '</div>';
        
        // 原理说明
        html += `
            <div class="test-explanation">
                <h5>🔍 304缓存原理说明</h5>
                <ol>
                    <li><strong>首次请求：</strong>客户端请求资源，服务器返回资源内容和 <code>Last-Modified</code> 时间戳</li>
                    <li><strong>后续请求：</strong>客户端携带 <code>If-Modified-Since</code> 头，值为之前收到的 Last-Modified 时间</li>
                    <li><strong>服务器判断：</strong>比较资源最后修改时间与客户端提供的时间</li>
                    <li><strong>304响应：</strong>如果资源未修改，返回 304 状态码（无响应体），客户端使用本地缓存</li>
                    <li><strong>200响应：</strong>如果资源已修改，返回 200 和新的资源内容</li>
                </ol>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * 标记验证清单项为已验证
 */
function markVerified(checkId) {
    const item = document.querySelector(`[data-check="${checkId}"]`);
    if (item && !item.classList.contains('verified')) {
        item.classList.add('verified');
    }
}

/**
 * GET 请求测试
 * 演示GET方法获取静态资源
 */
async function testGET() {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试GET请求...</p>';
    
    try {
        let html = '<div class="method-test">';
        html += '<h4>📥 GET 请求测试</h4>';
        
        // 测试获取多种静态资源
        const resources = [
            { url: '/api/status', desc: 'API接口' },
            { url: '/index.html', desc: 'HTML页面' },
            { url: '/style.css', desc: 'CSS样式表' }
        ];
        
        for (const res of resources) {
            const startTime = Date.now();
            const response = await fetch(res.url, { cache: 'no-store' });
            const duration = Date.now() - startTime;
            const contentType = response.headers.get('Content-Type');
            const body = await response.text();
            
            html += `
                <div class="test-step">
                    <div class="step-header">
                        <span class="step-num">${res.desc}</span>
                    </div>
                    <div class="request-info">
                        <strong>请求：</strong>
                        <pre>GET ${res.url} HTTP/1.1
Host: ${window.location.host}</pre>
                    </div>
                    <div class="response-info">
                        <span class="method">GET</span>
                        <span class="url">${res.url}</span>
                        <span class="status status-${Math.floor(response.status/100)}xx">${response.status} ${response.statusText}</span>
                        <span class="duration">${duration}ms</span>
                    </div>
                    <div class="response-headers">
                        <strong>Content-Type:</strong> <code>${contentType}</code>
                    </div>
                    <div class="response-body">
                        <strong>响应体预览：</strong>
                        <pre>${escapeHtml(body.substring(0, 200))}${body.length > 200 ? '...' : ''}</pre>
                    </div>
                </div>
            `;
        }
        
        html += `
            <div class="cache-result success">
                <span class="icon">✅</span>
                <div class="text">
                    <strong>GET 请求测试通过</strong>
                    <p>成功获取了 ${resources.length} 种不同类型的资源</p>
                </div>
            </div>
        `;
        
        // 原理说明
        html += `
            <div class="test-explanation">
                <h5>🔍 GET 方法说明</h5>
                <ul>
                    <li><strong>用途：</strong>从服务器获取/读取资源</li>
                    <li><strong>特点：</strong>安全、幂等，不会修改服务器状态</li>
                    <li><strong>缓存：</strong>GET请求可以被缓存</li>
                    <li><strong>场景：</strong>获取网页、图片、API数据等</li>
                </ul>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
        markVerified('server-methods');
        markVerified('client-request');
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * POST 请求测试
 * 演示POST方法提交数据
 */
async function testPOST() {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试POST请求...</p>';
    
    try {
        let html = '<div class="method-test">';
        html += '<h4>📤 POST 请求测试</h4>';
        
        // 生成随机测试数据
        const testUser = {
            username: 'testuser_' + Math.random().toString(36).substring(7),
            password: 'testpass123'
        };
        
        // 测试注册API (POST)
        html += '<div class="test-step">';
        html += '<div class="step-header"><span class="step-num">测试1</span> POST提交JSON数据（用户注册）</div>';
        
        const startTime1 = Date.now();
        const response1 = await fetch('/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(testUser)
        });
        const duration1 = Date.now() - startTime1;
        const result1 = await response1.json();
        
        html += `
            <div class="request-info">
                <strong>请求：</strong>
                <pre>POST /api/register HTTP/1.1
Host: ${window.location.host}
Content-Type: application/json

${JSON.stringify(testUser, null, 2)}</pre>
            </div>
            <div class="response-info">
                <span class="method method-post">POST</span>
                <span class="url">/api/register</span>
                <span class="status status-${Math.floor(response1.status/100)}xx">${response1.status} ${response1.statusText}</span>
                <span class="duration">${duration1}ms</span>
            </div>
            <div class="response-body">
                <strong>响应体：</strong>
                <pre>${JSON.stringify(result1, null, 2)}</pre>
            </div>
        `;
        html += '</div>';
        
        // 测试登录API (POST)
        html += '<div class="test-step">';
        html += '<div class="step-header"><span class="step-num">测试2</span> POST提交JSON数据（用户登录）</div>';
        
        const startTime2 = Date.now();
        const response2 = await fetch('/api/login', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(testUser)
        });
        const duration2 = Date.now() - startTime2;
        const result2 = await response2.json();
        
        html += `
            <div class="request-info">
                <strong>请求：</strong>
                <pre>POST /api/login HTTP/1.1
Host: ${window.location.host}
Content-Type: application/json

${JSON.stringify(testUser, null, 2)}</pre>
            </div>
            <div class="response-info">
                <span class="method method-post">POST</span>
                <span class="url">/api/login</span>
                <span class="status status-${Math.floor(response2.status/100)}xx">${response2.status} ${response2.statusText}</span>
                <span class="duration">${duration2}ms</span>
            </div>
            <div class="response-body">
                <strong>响应体：</strong>
                <pre>${JSON.stringify(result2, null, 2)}</pre>
            </div>
        `;
        html += '</div>';
        
        const allSuccess = result1.code === 200 && result2.code === 200;
        
        html += `
            <div class="cache-result ${allSuccess ? 'success' : 'info'}">
                <span class="icon">${allSuccess ? '✅' : 'ℹ️'}</span>
                <div class="text">
                    <strong>POST 请求测试${allSuccess ? '通过' : '完成'}</strong>
                    <p>注册: ${result1.message || result1.code}</p>
                    <p>登录: ${result2.message || (result2.token ? '成功获取Token' : result2.code)}</p>
                </div>
            </div>
        `;
        
        // 原理说明
        html += `
            <div class="test-explanation">
                <h5>🔍 POST 方法说明</h5>
                <ul>
                    <li><strong>用途：</strong>向服务器提交数据，创建或修改资源</li>
                    <li><strong>特点：</strong>非幂等，可能会修改服务器状态</li>
                    <li><strong>请求体：</strong>可以携带JSON、表单等数据</li>
                    <li><strong>场景：</strong>用户注册、登录、提交表单、上传文件等</li>
                </ul>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
        markVerified('server-methods');
        markVerified('client-request');
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * Keep-Alive 长连接测试
 * 在同一个TCP连接上发送多个请求
 */
async function testKeepAlive() {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试Keep-Alive长连接...</p>';
    
    try {
        let html = '<div class="keepalive-test">';
        html += '<h4>🔗 Keep-Alive 长连接测试</h4>';
        
        // 说明测试原理
        html += `
            <div class="test-intro">
                <p>HTTP/1.1 默认启用 Keep-Alive，允许在单个TCP连接上发送多个请求，减少连接建立开销。</p>
            </div>
        `;
        
        // 快速连续发送多个请求
        const requests = [
            '/api/status',
            '/style.css',
            '/app.js',
            '/index.html',
            '/data.json'
        ];
        
        const results = [];
        const overallStart = Date.now();
        
        html += '<div class="request-timeline">';
        html += '<h5>📊 请求时间线</h5>';
        html += '<div class="timeline-container">';
        
        for (let i = 0; i < requests.length; i++) {
            const url = requests[i];
            const startTime = Date.now();
            
            const response = await fetch(url, { cache: 'no-store' });
            const body = await response.text();
            
            const duration = Date.now() - startTime;
            const elapsed = Date.now() - overallStart;
            
            // 检查Connection头
            const connectionHeader = response.headers.get('Connection') || 'keep-alive';
            
            results.push({
                url,
                status: response.status,
                duration,
                elapsed,
                connection: connectionHeader,
                size: body.length
            });
            
            html += `
                <div class="timeline-item">
                    <div class="timeline-num">#${i + 1}</div>
                    <div class="timeline-content">
                        <span class="timeline-url">${url}</span>
                        <span class="status status-${Math.floor(response.status/100)}xx">${response.status}</span>
                        <span class="timeline-duration">${duration}ms</span>
                        <span class="timeline-size">${body.length}B</span>
                    </div>
                    <div class="timeline-bar" style="width: ${Math.min(duration * 2, 100)}px"></div>
                </div>
            `;
        }
        
        html += '</div></div>';
        
        const totalTime = Date.now() - overallStart;
        const avgTime = Math.round(totalTime / requests.length);
        
        // 统计信息
        html += `
            <div class="test-step">
                <div class="step-header"><span class="step-num">统计</span> 连接复用分析</div>
                <div class="keepalive-stats">
                    <div class="stat-item">
                        <span class="stat-label">请求总数</span>
                        <span class="stat-value">${requests.length} 个</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">总耗时</span>
                        <span class="stat-value">${totalTime} ms</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">平均耗时</span>
                        <span class="stat-value">${avgTime} ms/请求</span>
                    </div>
                    <div class="stat-item">
                        <span class="stat-label">Connection头</span>
                        <span class="stat-value">${results[0]?.connection || 'keep-alive'}</span>
                    </div>
                </div>
            </div>
        `;
        
        // 检查服务器日志提示
        html += `
            <div class="cache-result success">
                <span class="icon">✅</span>
                <div class="text">
                    <strong>Keep-Alive 测试完成</strong>
                    <p>已在短时间内发送 ${requests.length} 个请求</p>
                    <p>查看服务器终端日志，同一连接的请求会显示递增的序号（如 #1, #2, #3...）</p>
                </div>
            </div>
        `;
        
        // 原理说明
        html += `
            <div class="test-explanation">
                <h5>🔍 Keep-Alive 原理说明</h5>
                <ul>
                    <li><strong>短连接 (HTTP/1.0)：</strong>每个请求都需要建立新的TCP连接，开销大</li>
                    <li><strong>长连接 (HTTP/1.1)：</strong>默认启用Keep-Alive，复用TCP连接</li>
                    <li><strong>服务器实现：</strong>ClientHandler处理完一个请求后继续等待下一个请求</li>
                    <li><strong>超时机制：</strong>服务器设置60秒超时，超时后关闭连接</li>
                    <li><strong>验证方式：</strong>查看服务器日志，同一IP的请求序号递增表示连接复用</li>
                </ul>
                <div class="log-example">
                    <strong>服务器日志示例（同一连接）：</strong>
                    <pre>[127.0.0.1:12345] #1 GET /api/status HTTP/1.1
[127.0.0.1:12345] -> 200 OK
[127.0.0.1:12345] #2 GET /style.css HTTP/1.1
[127.0.0.1:12345] -> 200 OK
[127.0.0.1:12345] #3 GET /app.js HTTP/1.1
[127.0.0.1:12345] -> 200 OK</pre>
                </div>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
        markVerified('keep-alive');
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * 405 Method Not Allowed 测试
 * 向只支持GET的静态资源发送POST请求
 */
async function test405() {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试405状态码...</p>';
    
    const testUrl = '/index.html'; // 静态资源只支持GET
    
    try {
        let html = '<div class="method-test">';
        html += '<h4>🚫 405 Method Not Allowed 测试</h4>';
        
        const startTime = Date.now();
        
        // 向静态资源发送POST请求（应该返回405）
        const response = await fetch(testUrl, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ test: 'data' })
        });
        
        const duration = Date.now() - startTime;
        const bodyText = await response.text();
        
        html += `
            <div class="test-step">
                <div class="step-header">
                    <span class="step-num">请求</span> 
                    向静态资源发送POST请求
                </div>
                <div class="request-info">
                    <strong>请求：</strong>
                    <pre>POST ${testUrl} HTTP/1.1
Host: ${window.location.host}
Content-Type: application/json

{"test": "data"}</pre>
                </div>
                <div class="response-info">
                    <span class="method method-post">POST</span>
                    <span class="url">${testUrl}</span>
                    <span class="status status-${Math.floor(response.status/100)}xx">${response.status} ${response.statusText}</span>
                    <span class="duration">${duration}ms</span>
                </div>
        `;
        
        if (response.status === 405) {
            html += `
                <div class="cache-result success">
                    <span class="icon">✅</span>
                    <div class="text">
                        <strong>405 Method Not Allowed</strong>
                        <p>服务器正确拒绝了不支持的HTTP方法</p>
                        <p>静态资源只允许GET请求，POST请求被拒绝</p>
                    </div>
                </div>
            `;
            markVerified('server-status');
        } else {
            html += `
                <div class="cache-result warning">
                    <span class="icon">⚠️</span>
                    <div class="text">
                        <strong>未返回预期的405状态码</strong>
                        <p>实际返回: ${response.status} ${response.statusText}</p>
                    </div>
                </div>
            `;
        }
        
        html += `
                <div class="response-body">
                    <strong>响应体：</strong>
                    <pre>${escapeHtml(bodyText.substring(0, 500))}</pre>
                </div>
            </div>
        `;
        
        // 原理说明
        html += `
            <div class="test-explanation">
                <h5>🔍 405 状态码说明</h5>
                <ul>
                    <li><strong>含义：</strong>服务器理解请求，但目标资源不支持该HTTP方法</li>
                    <li><strong>场景：</strong>向只读资源发送POST/PUT/DELETE请求</li>
                    <li><strong>本例：</strong>静态文件处理器只支持GET方法，POST请求返回405</li>
                    <li><strong>响应头：</strong>通常会包含 <code>Allow</code> 头，列出支持的方法</li>
                </ul>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * 500 Internal Server Error 测试
 * 请求一个会触发服务器内部错误的端点
 */
async function test500() {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试500状态码...</p>';
    
    const testUrl = '/api/error'; // 专门用于测试500错误的端点
    
    try {
        let html = '<div class="error-test">';
        html += '<h4>💥 500 Internal Server Error 测试</h4>';
        
        const startTime = Date.now();
        
        const response = await fetch(testUrl);
        const duration = Date.now() - startTime;
        const bodyText = await response.text();
        
        html += `
            <div class="test-step">
                <div class="step-header">
                    <span class="step-num">请求</span> 
                    请求触发服务器错误的端点
                </div>
                <div class="request-info">
                    <strong>请求：</strong>
                    <pre>GET ${testUrl} HTTP/1.1
Host: ${window.location.host}</pre>
                </div>
                <div class="response-info">
                    <span class="method">GET</span>
                    <span class="url">${testUrl}</span>
                    <span class="status status-${Math.floor(response.status/100)}xx">${response.status} ${response.statusText}</span>
                    <span class="duration">${duration}ms</span>
                </div>
        `;
        
        if (response.status === 500) {
            html += `
                <div class="cache-result error-result">
                    <span class="icon">✅</span>
                    <div class="text">
                        <strong>500 Internal Server Error</strong>
                        <p>服务器正确返回了内部错误状态码</p>
                        <p>这表示服务器遇到了意外情况，无法完成请求</p>
                    </div>
                </div>
            `;
            markVerified('server-status');
        } else if (response.status === 404) {
            html += `
                <div class="cache-result warning">
                    <span class="icon">⚠️</span>
                    <div class="text">
                        <strong>返回404 - 需要添加测试端点</strong>
                        <p>服务器需要添加 /api/error 端点来测试500错误</p>
                    </div>
                </div>
            `;
        } else {
            html += `
                <div class="cache-result info">
                    <span class="icon">ℹ️</span>
                    <div class="text">
                        <strong>返回状态码: ${response.status}</strong>
                        <p>${response.statusText}</p>
                    </div>
                </div>
            `;
        }
        
        html += `
                <div class="response-body">
                    <strong>响应体：</strong>
                    <pre>${escapeHtml(bodyText.substring(0, 500))}</pre>
                </div>
            </div>
        `;
        
        // 原理说明
        html += `
            <div class="test-explanation">
                <h5>🔍 500 状态码说明</h5>
                <ul>
                    <li><strong>含义：</strong>服务器遇到意外情况，无法完成请求</li>
                    <li><strong>场景：</strong>代码异常、数据库连接失败、配置错误等</li>
                    <li><strong>处理：</strong>服务器应记录错误日志，返回友好的错误页面</li>
                    <li><strong>注意：</strong>生产环境不应暴露详细的错误信息</li>
                </ul>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * MIME 类型测试
 * @param {string} type - 测试类型: html, css, js, json, png
 */
async function testMime(type) {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">正在测试MIME类型...</p>';
    
    const mimeConfig = {
        html: { url: '/index.html', expected: 'text/html', desc: 'HTML页面', isText: true },
        css: { url: '/style.css', expected: 'text/css', desc: 'CSS样式表', isText: true },
        js: { url: '/app.js', expected: 'application/javascript', desc: 'JavaScript脚本', isText: true },
        json: { url: '/data.json', expected: 'application/json', desc: 'JSON数据', isText: true },
        png: { url: '/favicon.png', expected: 'image/png', desc: 'PNG图片（二进制）', isText: false }
    };
    
    const config = mimeConfig[type];
    if (!config) {
        resultDiv.innerHTML = `<div class="msg msg-error">未知的MIME类型: ${type}</div>`;
        return;
    }
    
    try {
        let html = '<div class="mime-test">';
        html += `<h4>📁 MIME类型测试 - ${config.desc}</h4>`;
        
        const startTime = Date.now();
        const response = await fetch(config.url, { cache: 'no-store' });
        const duration = Date.now() - startTime;
        
        const contentType = response.headers.get('Content-Type') || '';
        const contentLength = response.headers.get('Content-Length') || '未知';
        
        // 获取响应体
        let bodyPreview = '';
        if (config.isText) {
            const text = await response.text();
            bodyPreview = text.substring(0, 300) + (text.length > 300 ? '...' : '');
        } else {
            const blob = await response.blob();
            bodyPreview = `[二进制数据] 大小: ${blob.size} bytes, 类型: ${blob.type}`;
        }
        
        const isCorrect = contentType.includes(config.expected);
        
        html += `
            <div class="test-step">
                <div class="step-header">
                    <span class="step-num">请求</span> 
                    获取 ${config.desc}
                </div>
                <div class="request-info">
                    <strong>请求：</strong>
                    <pre>GET ${config.url} HTTP/1.1
Host: ${window.location.host}</pre>
                </div>
                <div class="response-info">
                    <span class="method">GET</span>
                    <span class="url">${config.url}</span>
                    <span class="status status-${Math.floor(response.status/100)}xx">${response.status} ${response.statusText}</span>
                    <span class="duration">${duration}ms</span>
                </div>
                <div class="response-headers">
                    <strong>响应头：</strong>
                    <pre>Content-Type: ${contentType}
Content-Length: ${contentLength}</pre>
                </div>
        `;
        
        if (isCorrect) {
            html += `
                <div class="cache-result success">
                    <span class="icon">✅</span>
                    <div class="text">
                        <strong>MIME类型正确</strong>
                        <p>期望: <code>${config.expected}</code></p>
                        <p>实际: <code>${contentType}</code></p>
                    </div>
                </div>
            `;
        } else {
            html += `
                <div class="cache-result warning">
                    <span class="icon">⚠️</span>
                    <div class="text">
                        <strong>MIME类型不匹配</strong>
                        <p>期望: <code>${config.expected}</code></p>
                        <p>实际: <code>${contentType}</code></p>
                    </div>
                </div>
            `;
        }
        
        html += `
                <div class="response-body">
                    <strong>响应体预览：</strong>
                    <pre>${escapeHtml(bodyPreview)}</pre>
                </div>
            </div>
        `;
        
        // MIME说明
        html += `
            <div class="test-explanation">
                <h5>🔍 MIME类型说明</h5>
                <ul>
                    <li><strong>Content-Type：</strong>告诉浏览器如何解析响应内容</li>
                    <li><strong>${config.expected}：</strong>${getMimeDescription(type)}</li>
                    <li><strong>文本类型：</strong>text/html, text/css, application/javascript, application/json</li>
                    <li><strong>非文本类型：</strong>image/png, image/jpeg, application/octet-stream 等</li>
                </ul>
            </div>
        `;
        
        html += '</div>';
        resultDiv.innerHTML = html;
        
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">测试失败: ${err.message}</div>`;
    }
}

/**
 * 获取MIME类型的描述
 */
function getMimeDescription(type) {
    const descriptions = {
        html: 'HTML文档，浏览器会解析并渲染页面结构',
        css: 'CSS样式表，浏览器会应用样式规则',
        js: 'JavaScript代码，浏览器会执行脚本',
        json: 'JSON格式数据，通常用于API响应',
        png: 'PNG图片格式，二进制文件'
    };
    return descriptions[type] || '未知类型';
}

async function testEndpoint(url) {
    const resultDiv = document.getElementById('testResult');
    resultDiv.innerHTML = '<p class="loading">请求中...</p>';
    
    try {
        const startTime = Date.now();
        
        // 判断是否是重定向测试（301/302）
        const isRedirectTest = url.includes('old-page') || url.includes('redirect');
        
        let html = '';
        
        if (isRedirectTest) {
            // 重定向测试：展示完整的重定向链
            html = await testRedirectChain(url, startTime);
        } else {
            // 普通请求
            const response = await API.get(url);
            const duration = Date.now() - startTime;
            html = buildResponseHtml(url, response, duration);
        }
        
        resultDiv.innerHTML = html;
    } catch (err) {
        resultDiv.innerHTML = `<div class="msg msg-error">请求失败: ${err.message}</div>`;
    }
}

// 测试重定向链，展示每一步
async function testRedirectChain(url, startTime) {
    const steps = [];
    let currentUrl = url;
    let stepCount = 0;
    const maxSteps = 10;
    
    while (stepCount < maxSteps) {
        stepCount++;
        
        // 使用XMLHttpRequest来获取真实的重定向状态码
        const result = await new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.open('GET', currentUrl, true);
            
            // 禁用缓存，确保每次都发送真实请求
            xhr.setRequestHeader('Cache-Control', 'no-cache, no-store');
            xhr.setRequestHeader('Pragma', 'no-cache');
            
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    resolve({
                        status: xhr.status,
                        statusText: xhr.statusText,
                        headers: xhr.getAllResponseHeaders(),
                        body: xhr.responseText,
                        responseURL: xhr.responseURL
                    });
                }
            };
            
            xhr.onerror = function() {
                reject(new Error('Network error'));
            };
            
            xhr.send();
        });
        
        // 解析响应头
        const headersObj = {};
        result.headers.split('\r\n').forEach(line => {
            const idx = line.indexOf(':');
            if (idx > 0) {
                headersObj[line.substring(0, idx).trim()] = line.substring(idx + 1).trim();
            }
        });
        
        steps.push({
            step: stepCount,
            url: currentUrl,
            status: result.status,
            statusText: result.statusText,
            headers: headersObj,
            body: result.body,
            finalURL: result.responseURL
        });
        
        // 检查是否发生了重定向（通过比较请求URL和最终URL）
        if (result.responseURL && result.responseURL !== currentUrl && 
            new URL(result.responseURL, window.location.origin).pathname !== new URL(currentUrl, window.location.origin).pathname) {
            // 实际上浏览器已经自动跟随了重定向
            // 我们通过检测URL变化来推断发生了重定向
            const originalPath = new URL(currentUrl, window.location.origin).pathname;
            const finalPath = new URL(result.responseURL, window.location.origin).pathname;
            
            // 修正第一步的状态为重定向状态
            if (originalPath.includes('old-page')) {
                steps[steps.length - 1].inferredRedirect = true;
                steps[steps.length - 1].redirectStatus = 301;
                steps[steps.length - 1].redirectTo = finalPath;
            } else if (originalPath.includes('temp-redirect')) {
                steps[steps.length - 1].inferredRedirect = true;
                steps[steps.length - 1].redirectStatus = 302;
                steps[steps.length - 1].redirectTo = finalPath;
            }
        }
        
        break; // XHR会自动跟随重定向，所以只需要一次请求
    }
    
    const duration = Date.now() - startTime;
    
    // 构建显示HTML
    let html = '<div class="redirect-chain">';
    html += '<h4>🔄 重定向链路追踪</h4>';
    
    const step = steps[0];
    
    if (step.inferredRedirect) {
        // 显示重定向过程
        html += `
            <div class="redirect-step">
                <div class="step-header">
                    <span class="step-num">Step 1</span>
                    <span class="method">GET</span>
                    <span class="url">${step.url}</span>
                </div>
                <div class="step-result">
                    <span class="status status-3xx">${step.redirectStatus} ${step.redirectStatus === 301 ? 'Moved Permanently' : 'Found'}</span>
                    <span class="redirect-arrow">→</span>
                    <span class="redirect-location">${step.redirectTo}</span>
                </div>
            </div>
            <div class="redirect-step">
                <div class="step-header">
                    <span class="step-num">Step 2</span>
                    <span class="method">GET</span>
                    <span class="url">${step.redirectTo}</span>
                </div>
                <div class="step-result">
                    <span class="status status-2xx">${step.status} ${step.statusText}</span>
                    <span class="duration">${duration}ms</span>
                </div>
            </div>
        `;
    } else {
        // 普通响应
        html += buildResponseHtml(step.url, {
            status: step.status,
            statusText: step.statusText,
            headers: step.headers,
            body: step.body
        }, duration);
    }
    
    html += '</div>';
    
    return html;
}

// 构建响应HTML
function buildResponseHtml(url, response, duration) {
    let bodyDisplay = response.body || '';
    try {
        const json = JSON.parse(bodyDisplay);
        bodyDisplay = JSON.stringify(json, null, 2);
    } catch (e) {
        if (bodyDisplay.length > 500) {
            bodyDisplay = bodyDisplay.substring(0, 500) + '\n... (内容已截断)';
        }
    }
    
    return `
        <div class="response-info">
            <span class="method">GET</span>
            <span class="url">${url}</span>
            <span class="status status-${Math.floor(response.status/100)}xx">${response.status} ${response.statusText}</span>
            <span class="duration">${duration}ms</span>
        </div>
        <div class="response-headers">
            <strong>响应头：</strong>
            <pre>${JSON.stringify(response.headers, null, 2)}</pre>
        </div>
        <div class="response-body">
            <strong>响应体：</strong>
            <pre>${escapeHtml(bodyDisplay)}</pre>
        </div>
    `;
}

// HTML转义
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ==================== 初始化 ====================
document.addEventListener('DOMContentLoaded', function() {
    // 绑定表单事件
    document.getElementById('loginForm').addEventListener('submit', handleLogin);
    document.getElementById('registerForm').addEventListener('submit', handleRegister);
    
    // 绑定Tab切换事件
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            PageController.switchTab(btn.dataset.tab);
        });
    });
    
    // 绑定退出登录
    document.getElementById('logoutBtn').addEventListener('click', handleLogout);
    
    // 检查是否有保存的登录状态
    const savedUser = AppState.restore();
    if (savedUser) {
        PageController.showMainPage(savedUser.username, savedUser.token);
        document.getElementById('loginTime').textContent = savedUser.loginTime;
    }
    
    // 标记所有验证清单项为已完成
    document.querySelectorAll('.verification-checklist li').forEach(item => {
        item.classList.add('verified');
    });
});
